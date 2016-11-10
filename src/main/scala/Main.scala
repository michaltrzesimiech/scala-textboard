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
import spray.json.DefaultJsonProtocol

case class Thread(threadId: Int, subject: String, createdAt: DateTime)
case class Post(threadId: Int, postId: Int, secretIt: Int, email: String, pseudonym: String, createdAt: DateTime)

object TextboardDb {
  case class CreateThread(thread: Thread)
  case class CreatePost(post: Post)
  case class EditPost(post: Post)
  case class DeletePost(post: Post)
  case object ListAllThreads
}

/** [Temporary] orchestrates actor behavior in relation to objects */
class TextboardDb extends Actor {
  import TextboardDb._
  var threads: Map[Thread, Post] = Map.empty

  def receive = {
    case CreateThread(thread) => println("thread created")
    case CreatePost(post)     => println("post created")
    case EditPost(post: Post) => println("post edited")
    case DeletePost(post)     => println("post deleted")
  }
}

/** Pulls in all implicit conversions to build JSON format instances, both RootJsonReader and RootJsonWriter. */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val threadFormat = jsonFormat2(Thread)
  implicit val postFormat = jsonFormat2(Post)
}

/** Utilizes Akka HTTP high-level API to define DSL routes and orchestrate flow */
object WebService extends App with TextboardJsonProtocol with SprayJsonSupport {

  implicit val system = ActorSystem("inviter")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher // execution context
  implicit val executor = system.dispatcher

  import akka.pattern.ask
  //  implicit val timeout = Timeout(5 seconds)

  val inviter = system.actorOf(Props[InvitationDb], name = "inviter")

  /**
   *  TODO: Set DSL routes, bind with server session
   *  Inbound: HttpRequest outbound: Future[HttpResponse]
   */

  val routes: Route = {
    ...    
  }
  
}  
