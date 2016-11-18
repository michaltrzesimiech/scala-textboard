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
import scala.collection.mutable.{ Seq, HashMap }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import spray.json._
import spray.json.DefaultJsonProtocol

/** add companion objects, then apply to protocols */

/** Domain model */
case class Thread[Post](
  subject: String,
  posts: HashMap[Int, Post])

case class Post(
  pseudonym: String,
  email: String,
  content: String)

/** DB actor*/
object TextboardDb {
  case class CreateThread(thread: Thread[Post])
  case class OpenThread(id: Int)
  case class DeleteThread(id: Int)
  case object ListAllThreads
  case class AddPost(threadId: Int, post: Post)
  case class EditPost(threadId: Int, postId: Int, newPost: Post)
  case class DeletePost(threadId: Int, postId: Int)
}

class TextboardDb extends Actor {
  import TextboardDb._

  def receive = {
    case CreateThread(thread)                => Universe.createThread(thread)
    case OpenThread(id)                      => Universe.openThread(id)
    case DeleteThread(id)                    => Universe.deleteThread(id)
    case ListAllThreads                      => Universe.threads.toList
    case AddPost(threadId, post)             => Universe.addPost(threadId, post)
    case EditPost(threadId, postId, newPost) => Universe.editPost(threadId, postId, newPost)
    case DeletePost(threadId, postId)        => Universe.deletePost(threadId: Int, postId: Int)
  }
}

/** DB operations */
object Universe {
  var threads: HashMap[Int, Thread[Post]] = HashMap.empty

  implicit def thisThread(id: Int): Option[Thread[Post]] = { threads.get(id) }
  implicit def thisPost(threadId: Int, postId: Int): Option[HashMap[Int, Post]] = {
    thisThread(threadId) map (_.posts) filter (_.keys == postId)
  }

  implicit val nextThreadId: Int = { if (threads.nonEmpty) threads.last._1 + 1 else 1 }
  implicit def nextPostInt(threadId: Int): Int = {
    val lastId = threads.last._2.posts.last._1.toInt
    if (lastId.isDefined) lastId + 1 else 1
  }

  def createThread(thread: Thread[Post]) = { threads = threads += (nextThreadId -> thread) }

  def openThread(id: Int): Option[Thread[Post]] = { thisThread(id) }

  def deleteThread(id: Int) = { threads = threads -= id }

  def listAllThreads = { threads }

  def addPost(threadId: Int, post: Post) = {
    threads.get(threadId) map (_.posts += (nextPostInt(threadId) -> post))
  }

  def editPost(threadId: Int, postId: Int, newPost: Post) = {
    threads.get(threadId) map (_.posts) map (_.update(postId, newPost))
  }

  def deletePost(threadId: Int, postId: Int) = {
    threads.get(threadId) map (_.posts.remove(postId))
  }
}
