package textboard

import akka.actor._
import akka.actor.{ Actor, Props, ActorRef }
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ ActorMaterializer, Materializer }
import akka.util.Timeout
import textboard._
import textboard.utils._
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.{ implicitConversions, postfixOps }
import spray.json._

object Service extends TextboardJsonProtocol with SprayJsonSupport with ConfigHelper {

  import akka.pattern.ask
  import textboard.domain._

  import WebServer._
  import DAO._
  import DbActor._

  /**
   * Invokes ActorRef to run DB operations for method POST
   */
  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")
  implicit val timeout: Timeout = Timeout(5 seconds)

  /**
   * Returns Route for:
   * 1. PUT				/thread/:thread_id/posts/:post_id?secret=x
   * 2. DELETE		/thread/:thread_id/posts/:post_id?secret=x
   * 3. GET				/thread/:thread_id/posts?limit=x&offset=x
   * 4. GET				/thread/:thread_id/posts
   * 5. POST			/thread/:thread_id/posts
   * 6. GET				/threads?limit=x&offset=x
   * 7. GET				/threads
   * 8. POST			/threads
   *
   * @param system 	The implicit system to use for building routes
   * @param ec 			The	implicit execution context to use for routes
   * @param mater 	The implicit materializer to use for routes
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
        get /** all posts in specific thread, with flexible limit and offset - 3 */ {
          parameters('limit.as[Int], 'offset.as[Int]) { (limit, offset) =>
            complete(openThread(threadId, dbLimit, dbOffset).toJson)
          }
        } ~
          get /** all posts in specific thread, with predefined limit and offset - 4 */ {
            complete(openThread(threadId, dbLimit, dbOffset).toJson)
          } ~
          post /** reply to specific thread - 5 */ {
            entity(as[Post]) { post =>
              (master ? CreatePost(lastId, post)).mapTo[Post]
              /** TODO: test if secret id is displayed */
              complete(StatusCodes.Created -> post.secretId)
            }
          }
      } ~
      path("threads") {
        get /** all threads with flexible limit and offset - 6 */ {
          parameters('limit.as[Int], 'offset.as[Int]) { (limit, offset) =>
            complete(listAllThreadsPaginated(limit, offset).toJson)
          }
        } ~
          get /** all threads with predefined limit and offset - 7 */ {
            complete(listAllThreadsPaginated(dbLimit, dbOffset).toJson)
          } ~
          post /** new thread - 8 */ {
            entity(as[NewThread]) { thread =>
              (master ? CreateNewThread(thread)).mapTo[NewThread]
              /** TODO: test if secret id is displayed */
              complete(StatusCodes.Created -> thread.secretId)
            }
          }
      }
  }
}