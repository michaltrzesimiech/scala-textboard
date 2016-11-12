import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import scala.collection.mutable.Seq
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import spray.json._
import spray.json.DefaultJsonProtocol

/** Domain model */
case class Thread(
  threadId: Int,
  subject: String,
  posts: scala.collection.mutable.Seq[Post])

case class Post(
  postId: Int,
  secretId: Int,
  email: String,
  pseudonym: String,
  content: String,
  createdAt: String)

object TextboardDb {
  case class CreateThread(thread: Thread)
  case class AddPost(thread: Thread, post: Post)
  case class EditPost(thread: Thread, post: Post)
  case class DeletePost(thread: Thread, post: Post)
  case object ListAllThreads
  case class OpenThread(thread: Thread)
}

class TextboardDb extends Actor {
  import TextboardDb._
  var threads: scala.collection.mutable.Seq[Thread] = Seq()

  def receive = {
    case CreateThread(thread) => threads = threads :+ thread
    case AddPost(post)        => println("append new post to Thread.posts for Thread.threadId")
    case EditPost(post: Post) => println("overwrite value Post.content by Post.postId")
    case DeletePost(post)     => println("delete post by Post.postId")
    case ListAllThreads       => threads.toList
    case OpenThread(thread)   => println("list posts for a single thread by Thread.threadId")
  }
}

/** Pulls in all implicit conversions to build JSON format instances, both RootJsonReader and RootJsonWriter. */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {

  // implicit val threadFormat = jsonFormat9(Thread)
  // implicit val postFormat = jsonFormat6(Post)

  implicit object ThreadJsonFromat extends RootJsonFormat[Thread] {

    /** TODO: Add types */
    def write(t: Thread) =
      JsArray(JsNumber(t.threadId), JsString(t.subject) /*, ??? */ )

    def read(value: JsValue) = value match {
      case JsArray(JsNumber(t.threadId), JsString(t.subject) /*, ??? */ ) =>
        new Thread(threadId, subject, post)
      case _ => deserializationError("Color expected")
    }
  }

  implicit object PostJsonFromat extends RootJsonFormat[Post] { /** TODO: Build format for Post */ }

}

/** Core service. Invokes ActorSystem, materializes Actor, orchestrates DSL routes, binds to server, terminates server.  */

object Service extends App with TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  import scala.language.postfixOps

  implicit val system = ActorSystem("inviter")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val postMaster = system.actorOf(Props[TextboardDb], name = "postMaster")

  /** TODO: Set DSL routes least strict to most strict */

  val route: Route = {
    complete("OK")

    //		  path("thread") {
    //      post & entity(as[Thread]) { thread =>
    //        threadFut = (postMaster ? TextboardDb.CreateThread(thread)).map[ToResponseMarshallable]
    //        complete(threadFut)
    //      }
    //    }

    //		  POST /thread
    //		  POST /thread/:id/posts
    //		  PUT /posts/:secret_id
    //		  DELETE /posts/:secret_id
    //		  GET /threads?limit=x&offset=x
    //		  GET /thread/:thread_id/posts
  }

  val config = ConfigFactory.load()
  val logger = Logging(system, getClass)

  val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))

  println(s"Server running. Press any key to stop."); StdIn.readLine()
  binding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}  
