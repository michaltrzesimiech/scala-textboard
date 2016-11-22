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
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.UUID
import scala.collection.mutable.{ HashMap }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import scala.util.{ Try, Success, Failure }
import spray.json._

object TextboardRoutes extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  import scala.language.postfixOps

  /**
   *  TODO: Compose endpoints, http://movio.co/blog/composing-endpoints-with-spray/
   *
   *   you should start with the more specific routes first,
   *   and then let the logic fall into the default cases,
   *   if they exist, further down into the tree.
   */

  /**
   * POST /thread
   * POST /thread/:id/posts
   * PUT /posts/:secret_id
   * DELETE /posts/:secret_id
   * -GET /threads?limit=x&offset=x
   * -GET /thread/:thread_id/posts
   */

  import akka.pattern.ask

  /** Invoke ActorSystem, materializes Actor and execution context */
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  /** Summon DbActor */
  implicit val timeout = Timeout(5 seconds)
  val master = system.actorOf(Props[DbActor])

  val route: Route = {
    path("threads" / IntNumber) { id =>
      get {
        complete(master ? DbActor.FindThreadById(id)) match {
          case Some(thread) => thread.getOrElse(StatusCodes.NotFound -> "Thread doesn't exist")
          case None         => StatusCodes.NotFound -> "Thread doesn't exist"
        }
      } ~
        (post) {
          entity(as[Post]) { post =>
            val threadId = post.threadId
            val postId = post.postId
            DAO.createPost(post)
            complete("Added post ${postId} to thread ${threadId}")
          }
        }
    } ~ path("threads" /** TODO: ?limit=x&offset=x */ ) {
      get {
        complete("List all threads")
      } ~
        post {
          entity(as[Thread]) { thread =>
            master ? DbActor.CreateThread(thread)
            complete(s"Created ${thread}")
          }
        }
    }
  }

  //  val route: Route = {
  //    path("threads") {
  //      get {
  //        val allThreads = DAO.listAllThreads
  //        complete(s"Here's a list of all threads: ${allThreads}")
  //      } ~
  //        post {
  //          entity(as[Thread]) { thread =>
  //            DAO.createThread(thread)
  //            complete(s"Created ${thread}")
  //          }
  //        }
  //    } ~
  //      path("threads" / "reply") {
  //        post {
  //          entity(as[Post]) { post =>
  //            val threadId = post.threadId
  //            val postId = post.postId
  //            DAO.createPost(post)
  //            complete("Added post ${postId} to thread ${threadId}")
  //          }
  //        }
  //      }
  //  }

  def run = {
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

