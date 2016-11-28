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
import com.wix.accord.dsl._
import scala.reflect.ClassTag

object Service extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import DbActor._
  import WebServer._

  /**
   *  Summons DbActor
   */
  implicit val timeout: Timeout = Timeout(10 seconds)
  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")

  /**
   * Returns the routes defined for endpoints:
   * 1. PUT            /thread/:thread_id/posts/:post_id?secret_id=x
   * 2. DELETE    /thread/:thread_id/posts/:post_id?secret_id=x
   * 3. GET            /thread/:thread_id/posts
   * 4. POST        /thread/:thread_id/posts
   * 5. GET            /threads?limit=x&offset=x
   * 6. POST        /thread
   *
   * @param system The implicit system to use for building routes
   * @param ec The implicit execution context to use for routes
   * @param mater The implicit materializer to use for routes
   */
  def route(implicit system: ActorSystem,
            ec: ExecutionContext,
            mater: Materializer): Route = {
    path("thread" / IntNumber / "posts" / IntNumber) { (threadId, postId) =>
      parameter('secret.as[String]) { secret =>
        put /** edit upon existing post in thread - 1 */ {
          entity(as[Post]) { post =>
            complete((master ? EditPost(postId, secret, post.content)).mapTo[ToResponseMarshallable])
          }
        } ~
          delete /** post in thread - 2 */ {
            (master ? DeletePost(postId, secret))
            complete(StatusCodes.OK)
          }
      }
    } ~
      path("thread" / IntNumber / "posts") { threadId =>
        get /** all posts in specific thread - 3 */ {
          complete((master ? OpenThread(threadId.toLong)).mapTo[Seq[Post]])
        } ~
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              complete((master ? CreatePost(post.threadId, post.pseudonym, post.email, post.content)).mapTo[Post])
            }
          }
      } ~
      path("threads") {
        /** TODO Make this print out an unordered list of threads */
        get /** all threads - 5 */ {
          val query = (master ? SimplyListAllThreads).mapTo[Seq[Thread]]
          complete(query)

          // parameters('limit.as[Int] ?, 'offset.as[Int] ?) { (limit, offset) =>
          // complete((master ? ListAllThreads(limit, offset)).mapTo[ToResponseMarshallable])
        }
      } ~
      post /** new thread - 6 */ {
        entity(as[NewThread]) { thread =>
          /*(master ? CreateThread(thread.threadId, thread.subject)).mapTo[Thread]
(master ? CreatePost(thread.threadId, thread.pseudonym, thread.email, thread.content)).mapTo[Post]*/
          complete((master ? CreateNewThread(thread.subject, thread.pseudonym, thread.email, thread.content)).mapTo[NewThread])
          // complete(Future.successful(StatusCodes.Created))
        }
      }
  }
}

/** TODO: deliver bare minimum functionality
*  1. Make all routes work
*  1a. with validation
*    2a. with verification of secret ID
*
*  X. Add basic routes for failures
*  X. Add pagination
*  X. Add indexes
*/