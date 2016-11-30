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
//import scala.util.{ Success, Failure }
import spray.json._

object Service extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import DbActor._
  import WebServer._

  /**
   *  Invokes DB Actor, with mandatory Timeout parameter
   */
  implicit val timeout: Timeout = Timeout(5 seconds)
  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")

  /**
   * Returns the routes defined for endpoints:
   * v1. PUT			/thread/:thread_id/posts/:post_id?secret=x
   * v2. DELETE		/thread/:thread_id/posts/:post_id?secret=x
   * v3. GET			/thread/:thread_id/posts
   * v4. POST			/thread/:thread_id/posts
   * v5. GET			/threads
   * x6. GET			/threads?limit=x&offset=x
   * v7. POST			/thread
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
        put /** edit upon existing post in thread - 1 */ {
          entity(as[NewContent]) { newContent =>
            DAO.editPost(secret, threadId, postId, newContent)
            // (master ? EditContent(secret, threadId, postId, newContent))
            log.info(s"Editing post $postId in thread $threadId with secret ${secret} handled OK")
            complete(StatusCodes.OK)
          }
        } ~
          delete /** post in thread - 2 */ {
            DAO.deletePost(secret, Some(postId))
            // (master ? DeletePost(secret, Some(postId)))
            log.info(s"Deleting post $postId in thread $threadId with secret ${secret} handled OK")
            complete(StatusCodes.OK)
          }
      }
    } ~
      path("thread" / LongNumber / "posts") { threadId =>
        get /** all posts in specific thread - 3 */ {
          complete(DAO.openThread(threadId).toJson)
        } ~
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              (master ? CreatePost(DAO.lastId, post)).mapTo[Post]
              complete(StatusCodes.Created)
            }
          }
      } ~
      path("threads") {
        get /** all threads with fixed max limit and offset - 5 */ {
          complete(DAO.listAllThreadsPaginated(config.getInt("pagination.limit"), config.getInt("pagination.offset")).toJson)
        } ~
          parameters('limit.as[Int], 'offset.as[Int]) { (limit, offset) =>
            get /** all threads with flexible limit and offset */ {
              complete(DAO.listAllThreadsPaginated(limit, offset).toJson)
            }
          }
      } ~
      post /** new thread - 6 */ {
        entity(as[NewThread]) { thread =>
          (master ? CreateNewThread(thread)).mapTo[NewThread]
          complete(StatusCodes.Created)
        }
      }
  }
}

/** 
 * TODO: 
 * 1. Make all routes work
 * = with validation
 * = with verification of secret ID, return secret while post is created
 * = with custom pagination
 */
