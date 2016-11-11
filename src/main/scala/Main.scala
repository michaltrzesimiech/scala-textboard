import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.http.scaladsl.Http
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
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import spray.json._
import spray.json.DefaultJsonProtocol

case class Thread(threadId: Int, subject: String, createdAt: String)
case class Post(threadId: Int, postId: Int, secretIt: Int, email: String, pseudonym: String, createdAt: String)

/** Domain model */
object TextboardDb {
  case class CreateThread(thread: Thread)
  case class CreatePost(post: Post)
  case class EditPost(post: Post)
  case class DeletePost(post: Post)
  case object ListAllThreads
  case class OpenThread(thread: Thread)
}

class TextboardDb extends Actor {
  import TextboardDb._
  var threads: Map[Thread, Post] = Map.empty

  def receive = {
    case CreateThread(thread) => println("thread created")
    case CreatePost(post)     => println("post created")
    case EditPost(post: Post) => println("post edited")
    case DeletePost(post)     => println("post deleted")
    case ListAllThreads       => threads.toList
    case OpenThread(thread)   => println("open a single thread by id")
  }
}

/** Pulls in all implicit conversions to build JSON format instances, both RootJsonReader and RootJsonWriter. */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val threadFormat = jsonFormat3(Thread)
  implicit val postFormat = jsonFormat6(Post)
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

  val inviter = system.actorOf(Props[TextboardDb], name = "inviter")

  /** TODO: Set DSL routes least strict to most strict */

  val routes: Route = {
    complete("OK")
    //		  POST /thread
    //		  POST /thread/:id/posts
    //		  PUT /posts/:secret_id
    //		  DELETE /posts/:secret_id
    //		  GET /threads?limit=x&offset=x
    //		  GET /thread/:thread_id/posts
  }

  val binding = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server running. Press any key to stop."); StdIn.readLine()
  binding
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}  
