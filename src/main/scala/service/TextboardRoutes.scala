package service

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
import scala.collection.mutable.{ Seq, Map, IndexedSeq, ArraySeq, HashMap, MutableList }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import spray.json._

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
  def mockCreateThread(thread: Thread) = {
    Universe.threads = Universe.threads :+ thread
    Universe.threads last
  }

  val route: Route = {
    path("threads") {
      get {
        complete(Universe.listAllThreads)
      }
    } ~
      post {
        entity(as[Thread]) { thread =>
          complete(
            mockCreateThread(thread))
        }
      }
  }

  // val route: Route = {
  // get {
  // path("thread" / IntNumber) { id =>
  // val maybeThread: Future[Option[Thread]] = Universe.openThread(id)
  // onSuccess(maybeThread) {
  // case Some(item) => complete(item)
  // case None => complete(StatusCodes.NotFound)
  // }
  // } ~ path("thread") {
  // complete(Universe.listAllThreads)
  // }
  // } ~
  // post {
  // path("thread" / "post") {
  // entity(as[List[Thread]]) { s =>
  // (boardMaster ? Universe.createThread(pseudonym, email, subject, content))
  // complete("thread created")
  // }
  // }
  // }
  // }

  // POST /thread
  // POST /thread/:id/posts
  // PUT /posts/:secret_id
  // DELETE /posts/:secret_id
  // GET /threads?limit=x&offset=x
  // GET /thread/:thread_id/posts

  def run = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(5 seconds)

    val boardMaster = system.actorOf(Props[TextboardDb])

    val logger = Logging(system, getClass)
    val config = ConfigFactory.load()
    val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))

    println(s"Server running. Press ENTER to stop."); StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}
