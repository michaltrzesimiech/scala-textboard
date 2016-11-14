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
import scala.collection.mutable.Seq
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import spray.json._
import spray.json.DefaultJsonProtocol
import java.util.UUID

/** Domain model */
case class Thread(
  threadId: Int,
  subject: String,
  posts: scala.collection.mutable.Seq[Map[Int, Post]])

case class Post(
  // postId: Int,
  // secretId: Int,
  pseudonym: String,
  email: String,
  content: String)

/** Pulls in all implicit conversions to build JSON format instances, both RootJsonReader and RootJsonWriter. */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val postFormat = jsonFormat3(Post)
  implicit object ThreadJsonFromat extends RootJsonFormat[Thread] {

    /** JSON => Thread */
    def read(value: JsValue) = value match {
      case obj: JsObject if (obj.fields.size == 2) => value.convertTo[Thread]
      case _                                       => deserializationError("Thread expected")
    }

    /** Thread => JSON (JsArray(JsNumber(t.threadId), JsString(t.subject), JsObject(t.posts))) */
    def write(t: Thread) = t.toJson
  }

  implicit object PostJsonFromat extends RootJsonFormat[Post] { /** TODO: Build format for Post */ }

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

  /**
   * val route: Route = {
   * get {
   * path("thread" / Int) { id =>
   * val maybeThread: Future[Option[Thread]] = Universe.openThread(id)
   *
   * onSuccess(maybeThread) {
   * case Some(item) => complete(item)
   * case None => complete(StatusCodes.NotFound)
   * }
   * }
   * } ~
   * post {
   * entity(as[Thread]) { thread =>
   * val saved: Future[Done] = createThread(thread)
   * onComplete(saved) { done =>
   * complete("thread created")
   * }
   * }
   * }
   * }
   *
   * // POST /thread
   * // POST /thread/:id/posts
   * // PUT /posts/:secret_id
   * // DELETE /posts/:secret_id
   * // GET /threads?limit=x&offset=x
   * // GET /thread/:thread_id/posts
   * }
   */

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
  case class OpenThread(threadId: Int)
  case object ListAllThreads
  case class AddPost(threadId: Int, email: String, pseudonym: String, content: String)
  case class EditPost(threadId: Int, postId: Int, content: String)
  case class DeletePost(threadId: Int)
}

class TextboardDb extends Actor {
  import TextboardDb._

  def receive = {
    case CreateThread(pseudonym, email, subject, content) => { Universe.createThread(pseudonym, email, subject, content) }
    case OpenThread(threadId)                             => Universe.openThread(threadId)
    case ListAllThreads                                   => Universe.threads.toList
    case AddPost(threadId, email, pseudonym, content)     => Universe.addPost(threadId, email, pseudonym, content)
    case EditPost(threadId, postId, content)              => Universe.editPost(threadId, postId, content)
    case DeletePost(threadId)                             => Universe.deletePost(threadId)
  }
}

object Universe {
  var threads: scala.collection.mutable.Seq[Thread] = Seq()

  val randomUUID = UUID.randomUUID() // TODO: add secret key as UUID
  val utilRandom = scala.util.Random
  val rangeA = 1 to 100
  val rangeB = 1000 to 5000
  val uniqueThreadId = rangeA(utilRandom.nextInt(rangeA length))
  val uniquePostId = rangeB(utilRandom.nextInt(rangeB length))

  def createThread(pseudonym: String, email: String, subject: String, content: String) = {
    var posts: scala.collection.mutable.Seq[Map[Int, Post]] = Seq() // always for a single thread

    /** begins post hierarchy, each post with unique ID */
    posts :+ (Map(uniquePostId -> Post(pseudonym, email, subject)))

    /** creates new thread with a unique ID */
    threads = threads :+ new Thread(uniqueThreadId, subject, posts)
  }

  /** Iterative substitute: for (t <- threads; if t.threadId == threadId) yield t */
  def openThread(threadId: Int) = { threads filter (_.threadId == threadId) }

  def addPost(threadId: Int, email: String, pseudonym: String, content: String) = {
    threads filter (_.threadId == threadId) map (_.posts :+ Map(uniquePostId -> Post(pseudonym, email, content)))
  }

  def editPost(threadId: Int, postId: Int, content: String) = {
    /** TODO: Use secret key as a condition */
    val postsUnderThread = threads filter (_.threadId == threadId) map (_.posts.toList)
  }

  def deletePost(threadId: Int) = {
    /** TODO: Use secret key as a condition */
    val postsUnderThread = threads filter (_.threadId == threadId) map (_.posts.toList)
  }
}
