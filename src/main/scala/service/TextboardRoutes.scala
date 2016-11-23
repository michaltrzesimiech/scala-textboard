package main.scala.textboard

import akka.actor._
import akka.actor.{ ActorSystem, Actor, Props, ActorRef }
import akka.Done
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.UUID
import scala.collection.mutable.{ HashMap }
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import scala.reflect.ClassTag
import scala.util.{ Try, Success, Failure }
import spray.json._

object TextboardRoutes extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask

  /** Invoke ActorSystem, materializes Actor and execution context */
  implicit val system = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mater: ActorMaterializer = ActorMaterializer()

  /** Summon DbActor */
  implicit val timeout: Timeout = Timeout(5 seconds)
  val master: ActorRef = system.actorOf(Props[DbActor])

  /**
   * Return the routes defined for endpoints:
   * 1. PUT         /thread/:thread_id/posts/:secret_id
   * 2. DELETE     /thread/:thread_id/posts/:secret_id
   * 3. GET         /thread/:thread_id/posts
   * 4. POST         /thread/:thread_id/posts
   * 5. GET         /threads?limit=x&offset=x
   * 6. POST         /thread
   *
   * @param system The implicit system to use for building routes
   * @param ec The implicit execution context to use for routes
   * @param mater The implicit materializer to use for routes
   */
  def route(implicit system: ActorSystem, ec: ExecutionContext, mater: Materializer): Route = {
    path("thread" / IntNumber / "posts" / IntNumber) { (threadId, postId) =>
      parameter('secret_id.as[Long]) { secret_id =>
        put /** upon existing post in thread - 1 */ {
          entity(as[Post]) { post =>
            // val futUpdatePost = (master ? DbActor.EditPost(postId, ${ secret_id }, post.content))
            complete(s"update post $postId in thread $threadId if secret ${secret_id} is OK")
          }
        } ~
          delete /** post in thread - 2 */ {
            // val futDeletePost = (master ? DbActor.DeletePost(postId, ${secret_id}))
            complete(s"delete post $postId in thread $threadId if secret ${secret_id} is OK")
          }
      }
    } ~
      path("thread" / IntNumber / "posts") { id =>
        get /** and open specific thread - 3 */ {
          val futOpenThread = (master ? DbActor.OpenThread(id)).mapTo[Thread]
          complete(futOpenThread)
        } ~
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              /** TODO: /IntNumer => post.threadId */
              val futCreatePost = (master ? DbActor.CreatePost(post)).mapTo[Post]
              /** TODO: print UUID */
              complete(futCreatePost)
            }
          }
      } ~
      path("thread") {
        (parameter('limit.as[Int]) & parameter('offset.as[Int])) { (limit, offset) =>
          get /** all threads - 5 */ {
            val futListAllThreads = (master ? DbActor.ListAllThreads(limit, offset)).mapTo[List[Thread]]
            complete(futListAllThreads)
          }
        }
      } ~
      post /** new thread - 6 */ {
        // respondWithMediaType(`application/json`) {}
        entity(as[Thread]) { thread =>
          val futPostThread = (master ? DbActor.CreateThread(thread)).mapTo[Thread]
          complete(futPostThread)
        }
      }
  }

  def run: Unit = {
    val log: LoggingAdapter = Logging(system, getClass)
    val config = ConfigFactory.load()

    /** Bind routes to server, gracefully terminate server when done */
    val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
    println(s"Server running. Press ENTER to stop."); StdIn.readLine()
    binding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

  }
}