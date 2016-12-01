package main.scala.textboard

import akka.actor._
import akka.actor.{ Actor, Props, ActorRef }
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
import java.util.UUID
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import scala.util.{ Try, Success, Failure }
import spray.json._

object Service extends TextboardJsonProtocol with SprayJsonSupport with ConfigHelper {

  import akka.pattern.ask
  import DbActor._
  import WebServer._
  import DAO._

  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")
  implicit val timeout: Timeout = Timeout(5 seconds)

  /**
   * Returns the routes defined for endpoints:
   * 
   * 1. PUT				/thread/:thread_id/posts/:post_id?secret=x
   * 2. DELETE		/thread/:thread_id/posts/:post_id?secret=x
   * 3. GET				/thread/:thread_id/posts
   * 4. POST			/thread/:thread_id/posts
   * 5. GET				/threads?limit=x&offset=x
   * 6. GET				/threads
   * 7. POST			/thread
   *
   * @param system The implicit system to use for building routes
   * @param ec The implicit execution context to use for routes
   * @param mater The implicit materializer to use for routes
   */
  def route(implicit system: ActorSystem,
            ec: ExecutionContext,
            mater: Materializer): Route = {
    path("thread" / LongNumber / "posts" / LongNumber) { (threadId, postId) =>
      parameter('secret.as[String]) { secret =>
        authorize(secretOk(Some(postId), secret)) {
          put /** upon post given secret is right - 1 */ {
            entity(as[NewContent]) { content =>
              editPost(threadId, postId, content)
              complete(StatusCodes.OK)
            }
          } ~
            delete /** post given secret is right - 2 */ {
              deletePost(Some(postId))
              complete(StatusCodes.OK)
            }
        }
      }
    } ~
      path("thread" / LongNumber / "posts") { threadId =>
        get /** all posts in specific thread - 3 */ {
          complete(openThread(threadId).toJson)
        } ~
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              (master ? CreatePost(lastId, post)).mapTo[Post]
              complete(StatusCodes.Created)
            }
          }
      } ~
      path("threads") {
        get /** all threads with flexible limit and offset - 5 */ {
          parameters('limit.as[Int], 'offset.as[Int]) { (limit, offset) =>
            complete(listAllThreadsPaginated(limit, offset).toJson)
          }
        } ~
          get /** all threads with fixed limit and offset - 6 */ {
            complete(listAllThreadsPaginated(dbLimit, dbOffset).toJson)
          } ~
          post /** new thread - 7 */ {
            entity(as[NewThread]) { thread =>
              (master ? CreateNewThread(thread)).mapTo[NewThread]
              complete(StatusCodes.Created)
            }
          }
      }
  }
}


