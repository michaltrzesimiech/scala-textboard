package textboard

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
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import slick.driver.PostgresDriver.api._
import spray.json._
import spray.json.DefaultJsonProtocol
import slick.lifted.{ AbstractTable, Rep, ProvenShape }

/** Domain model */
case class Thread(
  threadId: Long,
  subject: Long)

object Thread

class Threads(tag: Tag) extends Table[Thread](tag, "THREADS") {
  def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
  def subject = column[String]("SUBJECT")

  /** TODO: http://stackoverflow.com/questions/31248316/value-is-not-a-member-of-slick-lifted-repoptionint */
  def * = (threadId, subject)
}

case class Post(
  postId: Long,
  pseudonym: String,
  email: String,
  content: String)
object Post

object DAO extends TableQuery(new Threads(_)) {
  val db = Database.forConfig("database")

  def findById(threadId: Long): Future[Option[Thread]] = {
    db.run(this.filter(_.threadId === threadId).result).map(_.headOption)
  }

  def create(account: Thread): Future[Thread] = {
    db.run(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = threadId)) += account)
  }

  def deleteById(threadId: Long): Future[Int] = {
    db.run(this.filter(_.threadId === threadId).delete)
  }

  //  var threads: HashMap[Long, Thread[Post]] = HashMap.empty
  //
  //  implicit def thisThread(id: Long): Option[Thread[Post]] = { threads.get(id) }
  //  implicit def thisPost(threadId: Long, postId: Long): Option[HashMap[Long, Post]] = {
  //    thisThread(threadId) map (_.posts) filter (_.keys == postId)
  //  }
  //
  //  implicit val nextThreadId: Long = { if (threads.nonEmpty) threads.last._1 + 1 else 1 }
  //  implicit def nextPostId(threadId: Long): Long = {
  //    val lastId = threads.last._2.posts.last._1.toLong
  //    if (lastId.isDefined) lastId + 1 else 1
  //  }
  //
  //  def createThread(thread: Thread[Post]) = { threads = threads += (nextThreadId -> thread) }
  //
  //  def openThread(id: Long): Option[Thread[Post]] = { thisThread(id) }
  //
  //  def deleteThread(id: Long) = { threads = threads -= id }
  //
  //  def listAllThreads = { threads }
  //
  //  def addPost(threadId: Long, post: Post) = {
  //    threads.get(threadId) map (_.posts += (nextPostId(threadId) -> post))
  //  }
  //
  //  def editPost(threadId: Long, postId: Long, newPost: Post) = {
  //    threads.get(threadId) map (_.posts) map (_.update(postId, newPost))
  //  }
  //
  //  def deletePost(threadId: Long, postId: Long) = {
  //    threads.get(threadId) map (_.posts.remove(postId))
  //  }
}

///** DB actor*/
//object TextboardDb {
//  case class CreateThread(thread: Thread)
//  case class OpenThread(id: Long)
//  case class DeleteThread(id: Long)
//  case object ListAllThreads
//  case class AddPost(threadId: Long, post: Post)
//  case class EditPost(threadId: Long, postId: Long, newPost: Post)
//  case class DeletePost(threadId: Long, postId: Long)
//}
//
//class TextboardDb extends Actor {
//  import TextboardDb._
//  def receive = {
//    case CreateThread(thread)                => DAO.createThread(thread)
//    case OpenThread(id)                      => DAO.openThread(id)
//    case DeleteThread(id)                    => DAO.deleteThread(id)
//    case ListAllThreads                      => DAO.threads.toList
//    case AddPost(threadId, post)             => DAO.addPost(threadId, post)
//    case EditPost(threadId, postId, newPost) => DAO.editPost(threadId, postId, newPost)
//    case DeletePost(threadId, postId)        => DAO.deletePost(threadId: Long, postId: Long)
//  }
//}
