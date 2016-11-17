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
import scala.collection.mutable.{ HashMap }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import spray.json._

object TextboardRoutes extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import akka.util.Timeout
  import scala.concurrent.duration._
  import scala.language.postfixOps

  val route: Route = {
    path("threads") {
      get {
        val allThreads = Universe.listAllThreads
        complete(s"Here's a list of all threads: ${allThreads}")
      } ~
        post {
          entity(as[Thread]) { thread =>
            /** (boardMaster ? Universe.createThread(thread, Post("a", "b", "c"))) */
            Universe.createThread(thread, Post("a", "b", "c"))
            complete(s"Created ${thread}")
          }
        }
    } ~
      path("threads" / "reply") {
        post {
          entity(as[Post]) { post =>
            val threadId = 1
            Universe.addPost(threadId, post)
            complete("Added ${post} to thread ${threadId}")
          }
        }
      }
  }

  /**
   * TODO: Set routes, least strict to most strict:
   *
   * POST /thread
   * POST /thread/:id/posts
   * PUT /posts/:secret_id
   * DELETE /posts/:secret_id
   * GET /threads?limit=x&offset=x
   * GET /thread/:thread_id/posts
   */

  def run = {
    import akka.pattern.ask

    /** Invoke ActorSystem, materializes Actor and execution context */
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    /** Summon DBActor */
    //    implicit val timeout = Timeout(5 seconds)
    //    val boardMaster = system.actorOf(Props[TextboardDb])

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

/**
* TODO: Break down
* 1. Enable creating Threads, listing all Threads
* 2. Enable opening Threads and deleting Threads by Id
* 3. Enable adding Posts to Threads
* 4. Enable editing, deleting Posts
* 5. Integrate with Postgres
* 6. Add secret key as condition to edit or delete Post, delete Thread
* 7. Pattern matching on stabilised threads for nextThreadId
*/