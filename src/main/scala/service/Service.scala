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
//import scala.util.{ Try, Success, Failure }
import spray.json._

object Service extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import DbActor._
  import WebServer._

  /**
   *  Summons DbActor
   */
  implicit val timeout: Timeout = Timeout(5 seconds)
  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")

  /**
   * Returns the routes defined for endpoints:
   * 1. PUT         /thread/:thread_id/posts/:post_id?secret_id=x
   * 2. DELETE     /thread/:thread_id/posts/:post_id?secret_id=x
   * 3. GET         /thread/:thread_id/posts
   * 4. POST         /thread/:thread_id/posts
   * 5. GET         /threads?limit=x&offset=x
   * 6. POST         /thread
   *
   * @param system The implicit system to use for building routes
   * @param ec The implicit execution context to use for routes
   * @param mater The implicit materializer to use for routes
   */
  def route(implicit system: ActorSystem,
            ec: ExecutionContext,
            mater: Materializer /*,
marshallToPost: ToResponseMarshaller[Post],
marshallToThread: ToResponseMarshaller[Thread],
unmarshallPost: FromRequestUnmarshaller[Post],
unmarshallThread: FromRequestUnmarshaller[Thread]*/ ): Route = {
    path("thread" / IntNumber / "posts" / IntNumber) { (threadId, postId) =>
      parameter('secret_id.as[String]) { secret_id =>
        put /** edit upon existing post in thread - 1 */ {
          entity(as[Post]) { post =>
            (master ? EditPost(threadId, postId, secret_id, post.content))
            // log.info(s"Editing post $postId in thread $threadId with secret ${secret_id} handled OK")
            complete(StatusCodes.OK)
          }
        } ~
          delete /** post in thread - 2 */ {
            (master ? DeletePost(postId, secret_id))
            // log.info(s"Deleting post $postId in thread $threadId with secret ${secret_id} handled OK")
            complete(Future.successful(StatusCodes.OK))
          }
      }
    } ~
      path("thread" / IntNumber / "posts") { threadId =>
        get /** all posts in specific thread - 3 */ {
          val formattedId = threadId.toLong
          val futOpenThread = (master ? OpenThread(formattedId)).mapTo[List[Post] /*ToResponseMarshallable*/ ]
          complete(futOpenThread)
        } ~
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              val formattedId = Some(threadId.toLong)
              val futCreatePost = (master ? CreatePost(
                formattedId,
                post.pseudonym,
                post.email,
                post.content)).mapTo[Post]
              val secretId = futCreatePost.map(_.secretId)
              complete(StatusCodes.Created -> s"(with secret ID $secretId)")
            }
          }
      } ~
      path("threads") {
        (parameter('limit.as[Int]) & parameter('offset.as[Int])) { (limit, offset) =>
          get /** all threads - 5 */ {
            val futListAllThreads = (master ? ListAllThreads(limit, offset)).mapTo[List[Thread]]
            complete(futListAllThreads)
          }
        }
      } ~
      post /** new thread - 6 */ {
        entity(as[Thread]) { thread =>
          (master ? CreateThread(thread)).mapTo[Thread]
          complete(Future.successful(StatusCodes.Created))
        }
      }
  }
}
