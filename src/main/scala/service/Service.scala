import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import scala.collection.mutable.{ Seq, Map }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import spray.json._
import spray.json.DefaultJsonProtocol
import java.util.UUID

/** Domain model */
case class Thread(
  threadId: AnyVal,
  subject: String,
  posts: scala.collection.mutable.Seq[Map[Int, Post]])

case class Post(
  // secretId: Int,
  postId: Any,
  pseudonym: String,
  email: String,
  content: String)

case class IndexedPost(
  postId: collection.mutable.Map[Any, Post])

/** Pulls in all implicit conversions to build JSON format instances, both RootJsonReader and RootJsonWriter. */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object ThreadJsonFromat extends RootJsonFormat[Thread] {

    /** JSON => Thread */
    def read(value: JsValue) = value match {
      case obj: JsObject if (obj.fields.size == 2) => value.convertTo[Thread]
      case _                                       => deserializationError("Thread expected")
    }

    /** Thread => JSON (JsArray(JsNumber(t.threadId), JsString(t.subject), JsObject(t.posts))) */
    def write(t: Thread) = t.toJson
  }

  implicit object PostJsonFromat extends RootJsonFormat[Post] {
    def read(value: JsValue) = value match {
      case obj: JsObject if (obj.fields.size == 2) => value.convertTo[Post]
      case _                                       => deserializationError("Post expected")
    }

    def write(p: Post) = p.toJson
  }
}

/** Core service. Invokes ActorSystem, materializes Actor, orchestrates DSL routes, binds to server, terminates server. */
object TextboardRoutes extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  import scala.language.postfixOps

  implicit val system = ActorSystem("inviter")
  implicit val timeout = Timeout(5 seconds)
  val threader = system.actorOf(Props[TextboardDb], name = "threader")

  /** TODO: Set DSL routes least strict to most strict */
  val route: Route = {
    get {
      path("thread" / IntNumber) { id =>
        val maybeThread: Future[Option[Thread]] = Universe.openThread(id)
        onSuccess(maybeThread) {
          case Some(item) => complete(item)
          case None       => complete(StatusCodes.NotFound)
        }
      } ~ path("thread") {
        complete(Universe.listAllThreads)
      }
    } ~
      post {
        path("thread" / "post") {
          entity(as[Thread]) { thread =>
            Universe.createThread(thread)
            complete("thread created")
          }
        }
      }

    // POST /thread
    // POST /thread/:id/posts
    // PUT /posts/:secret_id
    // DELETE /posts/:secret_id
    // GET /threads?limit=x&offset=x
    // GET /thread/:thread_id/posts
  }

  def run = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val logger = Logging(system, getClass)
    val config = ConfigFactory.load()
    val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))

    println(s"Server running. Press ENTER to stop."); StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

object WebServer extends App { TextboardRoutes.run }

object TextboardDb {
  case class CreateThread(pseudonym: String, email: String, subject: String, content: String)
  case class OpenThread(id: Int)
  case class DeleteThread(id: Int)
  case object ListAllThreads
  case class AddPost(threadId: Int, email: String, pseudonym: String, content: String)
  case class EditPost(threadId: Int, postId: Int, content: String)
  case class DeletePost(threadId: Int)
}

class TextboardDb extends Actor {
  import TextboardDb._

  def receive = {
    case CreateThread(pseudonym, email, subject, content) => { Universe.createThread(pseudonym, email, subject, content) }
    case OpenThread(id)                                   => Universe.openThread(id)
    case DeleteThread(id)                                 => Universe.deleteThread(id)
    case ListAllThreads                                   => Universe.threads.toList
    case AddPost(threadId, email, pseudonym, content)     => Universe.addPost(threadId, email, pseudonym, content)
    case EditPost(threadId, postId, content)              => Universe.editPost(threadId, postId, content)
    case DeletePost(threadId)                             => Universe.deletePost(threadId)
  }
}

object Universe {
  /** TODO: add secret key */
  val randomUUID = UUID.randomUUID()

  var threads: scala.collection.mutable.Seq[Thread] = Seq.empty
  val nextThreadId = { if (threads.nonEmpty) threads.last.threadId + 1 else 1 /** TODO: pattern matching on stabilised threads */ }
  implicit def thisThread(id: Int): Thread = { threads filter (_.threadId == id) head }

  implicit var posts: scala.collection.mutable.Seq[Map[Int, Post]] = Seq()
  implicit val nextPostId = { if (posts.nonEmpty) posts.last map (_._1 + 1) else 1 /** TODO: pattern matching on stabilised posts */ }

  def createThread(pseudonym: String, email: String, subject: String, content: String) = {
    /** adds new thread with post hierarchy to all threads */
    threads = threads :+ new Thread(nextThreadId, subject, posts)

    /** begins post hierarchy, each post gets unique ID */
    val postId = nextPostId
    val postIndexed = new Post(postId, pseudonym, email, content)
    threads.last.posts :+ new IndexedPost(Map(postId -> postIndexed))
  }

  def openThread(id: Int) = { thisThread(id) }

  def listAllThreads = { threads.toList }

  def deleteThread(id: Int) = {
    dropMatch(threads, thisThread(id))

    def dropMatch[Thread](ls: Seq[Thread], value: Thread): Seq[Thread] = {
      val index = ls.indexOf(value)
      if (index < 0) {
        ls
      } else if (index == 0) {
        ls.tail
      } else {
        val (a, b) = ls.splitAt(index)
        a ++ b.tail
      }
    }
  }

  def addPost(threadId: Int, email: String, pseudonym: String, content: String) = {
    val postId = nextPostId
    val postIndexed = new Post(postId, pseudonym, email, content)
    threads filter (_.threadId == threadId) map (_.posts :+ new IndexedPost(Map(postId -> postIndexed)))
  }

  /** TODO: fetch post by id in a concrete thread, use lenses */
  //  implicit def thisPost(threadId: Int, postId: Int): Post = {
  //    data.toMap.get('b').get
  //    data.find(_._1 == 'b').get._2
  //    myMap.mapValues(_.map(_._2))

  //    val postsInThread: Seq[Map[Int, Post]] = thisThread(threadId).posts filter (_._1 == postId) head
  //    val postsInThreadWithId: Seq[Map[Int, Post]] = for (post <- posts) yield post filter (_._1 == postId)
  //  }

  def editPost(threadId: Int, postId: Int, content: String) = {
    /** TODO: Use secret key as a condition */
    //    val postsInThread: Seq[Map[Int, Post]] = thisThread(threadId).posts
    //    val postsInThreadWithId: Seq[Map[Int, Post]] = for (post <- posts) yield post filter (_._1 == postId) map (_._2 => content)
    //    for (post <- postsInThreadWithId) yield post map (_._2 => content)
  }

  def deletePost(threadId: Int) = {
    /** TODO: Use secret key as a condition */
    //    val postsUnderThread = threads filter (_.threadId == threadId) map (_.posts.toList)
  }
}

/**
* TODO: Break down
* 1. Enable creating Threads with IndexedPosts, listing all Threads
* 2. Enable opening Threads and deleting Threads by Id
* 3. Enable adding Posts to Threads
* 4.    Enable editing, deleting Posts
* 5. Integrate with Postgres
*/