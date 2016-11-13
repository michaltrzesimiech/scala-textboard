/**
 * Building on a simpler model
 */

import akka.actor.ActorSystem
import akka.actor.{ Actor, ActorRef, Props }
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import spray.json._
import spray.json.DefaultJsonProtocol

import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.testkit.ScalatestRouteTest

trait InviterJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object InvitationJsonFromat extends RootJsonFormat[Invitation] {
    // when performing a GET
    def write(i: Invitation) =
      JsArray(JsString(i.invitee), JsString(i.email))

    // when performing a POST
    def read(value: JsValue) = value match {
      case JsArray(Vector(JsString(name), JsString(email))) =>
        new Invitation(name, email)
      case _ => deserializationError("Invitation expected")
    }
  }
}

object InviterRoutes extends InviterJsonProtocol with SprayJsonSupport {
  import akka.http.scaladsl.server.Directives._
  import akka.pattern.ask
  import akka.util.Timeout
  import InviterDb._
  import scala.concurrent.duration._
  import scala.language.postfixOps

  implicit val timeout = Timeout(5 seconds)

  /** Orchestrates DSL routes with an instance of "inviter" actor */
  def routes(inviter: ActorRef): Route = {
    path("invitation") {
      get {
        entity(as[List[Invitation]]) { invitation =>
          val futGet: Future[List[Invitation]] = (inviter ? InviterDb.FindAllInvitations).mapTo[List[Invitation]]
          complete(futGet)
        }
      }
    } ~
      post {
        entity(as[Invitation]) { invitation =>
          val futPost = (inviter ? InviterDb.CreateInvitation(invitation)).mapTo[Invitation]
          complete(futPost)
        }
      }
  }

  /** Invokes ActorSystem, materializes Actor, binds to server, terminates server.  */
  def run: Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5 seconds)
    val inviter = system.actorOf(Props[InviterDb], "inviter")

    //    val config = ConfigFactory.load()
    val logger = Logging(system, getClass)

    val serverSource: Source[Http.IncomingConnection, Future[Http.ServerBinding]] =
      Http().bind(interface = "localhost", port = 8081)
    val sink = Sink.foreach[Http.IncomingConnection](_.handleWith(routes(inviter)))
    serverSource.to(sink).run

    /**
     * val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
     * println(s"Server running. Press any key to stop."); StdIn.readLine()
     * binding
     * .flatMap(_.unbind())
     * .onComplete(_ => system.terminate())
     */
  }
}

object InviterServer extends App { import InviterRoutes._; run }

case class Invitation(invitee: String, email: String)
object Invitation

object InviterDb {
  case class CreateInvitation(invitation: Invitation)
  case object FindAllInvitations
}

class InviterDb extends Actor {
  import scala.collection.mutable.Seq
  import InviterDb._

  val invitation0 = Invitation("John Smith", "john@smith.mx")
  var invitations: Seq[Invitation] = Seq(invitation0)

  def receive = {
    case CreateInvitation(invitation) => invitations = invitations :+ invitation
    case FindAllInvitations           => invitations.toList
  }
}