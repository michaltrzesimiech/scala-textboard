package main.scala.textboard

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
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.UUID
import scala.collection.mutable.{ HashMap }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import scala.util.{ Try, Success, Failure }
import spray.json._

object TextboardRoutes extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask

  /** Invoke ActorSystem, materializes Actor and execution context */
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  /** Summon DbActor */
  implicit val timeout = Timeout(5 seconds)
  val master = system.actorOf(Props[DbActor])

  /**
   *  TODO: Compose endpoints,
   *  - http://movio.co/blog/composing-endpoints-with-spray/
   *  - "you should start with the more specific routes first,
   *   and then let the logic fall into the default cases,
   *   if they exist, further down into the tree."
   */

  /**
   * POST /thread
   * POST /thread/:id/posts
   * PUT /posts/:secret_id
   * DELETE /posts/:secret_id
   * -GET /threads?limit=x&offset=x
   * -GET /thread/:thread_id/posts
   */

  val route: Route = {
    path("threads" / IntNumber) { id =>
      get {
        val futFindThread = (master ? DbActor.FindThreadById).mapTo[Thread]
        complete(futFindThread)
      } ~
        (post) {
          entity(as[Post]) { post =>
            val threadId = post.threadId
            val postId = post.postId
            val futCreatePost = (master ? DbActor.CreatePost(post)).mapTo[Post]
            complete(futCreatePost)
          }
        }
    } ~ path("threads" /** TODO: ?limit=x&offset=x */ ) {
      get {
        val futListAllThreads = (master ? DbActor.ListAllThreads).mapTo[List[Thread]]
        complete(futListAllThreads)
      } ~
        post {
          entity(as[Thread]) { thread =>
            val futPostThread = (master ? DbActor.CreateThread(thread)).mapTo[Thread]
            complete(futPostThread)
          }
        }
    }
  }

  def run: Unit = {
    val logger = Logging(system, getClass)
    val config = ConfigFactory.load()

    /** Bind routes to server, gracefully terminate server when done */
    val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
    println(s"Server running. Press ENTER to stop."); StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }
}

