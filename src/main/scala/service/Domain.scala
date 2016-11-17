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
import scala.collection.mutable.{ Seq, Map, IndexedSeq, ArraySeq, HashMap, MutableList }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
//import shapeless._
import spray.json._
import spray.json.DefaultJsonProtocol

/** Domain model */
case class Thread(
  threadId: Int,
  subject: String,
  posts: HashMap[Int, Post])

case class Post(
  pseudonym: String,
  email: String,
  content: String)

object TextboardDb {
  case class CreateThread(pseudonym: String, email: String, subject: String, content: String)
  case class OpenThread(id: Int)
  case class DeleteThread(id: Int)
  case object ListAllThreads
  case class AddPost(threadId: Int, email: String, pseudonym: String, content: String)
  case class EditPost(threadId: Int, postId: Int, content: String)
  case class DeletePost(threadId: Int)
}

class TextboardDb extends Actor {
  import TextboardDb._

  def receive = {
    case CreateThread(pseudonym, email, subject, content) => Universe.createThread(pseudonym, email, subject, content)
    case OpenThread(id)                                   => Universe.openThread(id)
    case DeleteThread(id)                                 => Universe.deleteThread(id)
    case ListAllThreads                                   => Universe.threads.toList
    case AddPost(threadId, email, pseudonym, content)     => Universe.addPost(threadId, email, pseudonym, content)
    case EditPost(threadId, postId, content)              => Universe.editPost(threadId, postId, content)
    // case DeletePost(threadId, postId) => Universe.deletePost(threadId: Int, postId: Int)
  }
}

object Universe {
  var threads: MutableList[Thread] = MutableList.empty

  val nextThreadId = { if (threads.nonEmpty) threads.last.threadId + 1 else 1 }
  implicit def thisThread(id: Int): Option[Thread] = { threads.get(id) }

  def createThread(pseudonym: String, email: String, subject: String, content: String) = {
    /** adds new thread with post hierarchy to all threads */
    threads = threads :+ new Thread(nextThreadId, subject, HashMap.empty)

    /** begins post hierarchy, each post gets unique ID */
    threads.last.posts += (1 -> new Post(pseudonym, email, content))
  }

  def openThread(id: Int): Option[Thread] = { thisThread(id) }

  def deleteThread(id: Int) = { thisThread(id) map (_ => 0) }

  def listAllThreads = { threads.toList }

  /** TODO: simplify, potentially lenses */
  implicit def thisPost(threadId: Int, postId: Int): Option[HashMap[Int, Post]] = {
    val postsHere: Option[HashMap[Int, Post]] = threads.get(threadId) map (_.posts)
    val indexes: Option[Iterable[Int]] = postsHere map (_.keys)
    val thisId: Option[Int] = for (id <- indexes; if id == postId) yield id head
    val yourPost: Option[HashMap[Int, Post]] = postsHere filter (_.keys == thisId)
    yourPost
  }

  def addPost(threadId: Int, email: String, pseudonym: String, content: String) = {
    /** TODO: Get lenses here */
    val postIds: Option[Iterable[Int]] = threads.get(threadId) map (_.posts.keys)
    val lastId: Option[Int] = for (id <- postIds) yield id.last.toInt
    val nextPostInt = if (lastId.isDefined) lastId.last.toInt + 1 else 1

    threads.get(threadId) map (_.posts += (nextPostInt -> new Post(pseudonym, email, content)))
  }

  /** TODO: Use secret key as a condition */
  /** TODO: Get lenses here */
  def editPost(threadId: Int, postId: Int, content: String) = { thisPost(threadId, postId) map (x => x.-=(postId)) }

  /** TODO: Use secret key as a condition */
  def deletePost(threadId: Int, postId: Int) = { thisThread(threadId) map (_.posts.remove(postId)) }
}

/**
* TODO: Break down
* 1. Enable creating Threads, listing all Threads
* 2. Enable opening Threads and deleting Threads by Id
* 3. Enable adding Posts to Threads
* 4. Enable editing, deleting Posts
* 5. Integrate with Postgres
* 6. Add secret key to Post
* 7. Pattern matching on stabilised threads for nextThreadId
*/