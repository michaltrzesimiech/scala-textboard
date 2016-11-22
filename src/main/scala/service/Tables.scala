package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.event.{ LoggingAdapter, Logging }
//import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.model._
//import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
//import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import java.util.UUID
import scala.collection.mutable.{ Seq, HashMap }
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io._
//import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
//import slick.driver.PostgresDriver.backend._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import spray.json._
import spray.json.DefaultJsonProtocol

/** Threads table */
final class Threads(tag: Tag) extends Table[Thread](tag, "THREADS") {
  /** Auto Increment the threadId primary key column */
  def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
  def subject = column[String]("SUBJECT" /** TODO: add condition is not null */ )

  def * : ProvenShape[Thread] = (threadId.?, subject) <> ((Thread.apply _).tupled, Thread.unapply)

  /**
   * For primitive types:
   * class Threads(tag: Tag) extends Table[(Long, String)](tag, "THREADS") {
   * def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
   * def subject = column[String]("SUBJECT")
   * def * : ProvenShape[(Long, String)] = (threadId, subject) }
   */
}

/** Posts table */
final class Posts(tag: Tag) extends Table[Post](tag, "POSTS") {
  /** Auto Increment the threadId primary key column.*/
  def postId = column[Long]("POST_ID", O.PrimaryKey, O.AutoInc)
  def threadId = column[Long]("THR_ID")
  def secretId = column[UUID]("SECRET", O.AutoInc)
  def pseudonym = column[String]("PSEUDONYM")
  def email = column[String]("EMAIL")
  def content = column[String]("CONTENT")

  def * : ProvenShape[Post] = (postId.?, threadId.?, secretId.?, pseudonym, email, content) <> ((Post.apply _).tupled, Post.unapply)

  /** A reified foreign key relation that can be navigated to create a join */
  def thread = foreignKey("THR_FK", threadId, Thread.threads)(_.threadId)
}

case class Thread(
  /** Columns that are auto-incremented are automatically ignored. */
  threadId: Option[Long] = None,
  subject: String)

case class Post(
  postId: Option[Long] = None,
  threadId: Option[Long],
  secretId: Option[UUID],
  pseudonym: String,
  email: String,
  content: String)

object Thread {
  /** Query interface for the Threads table */
  val threads: TableQuery[Threads] = TableQuery[Threads]
}

object Post {
  /** Query interface for the Posts table */
  val posts: TableQuery[Posts] = TableQuery[Posts]
}

object DAO extends TableQuery(new Threads(_)) {

  implicit val db = Database.forConfig("database")

  val threads = Thread.threads
  val posts = Post.posts

  implicit def adapter(id: Int) = Some(id.toLong)

  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  val setup = DBIO.seq(
    /** Create tables, including primary and foreign keys */
    (threads.schema ++ posts.schema).create,

    /** Insert dummy threads. */
    threads += Thread(None, "one subject"),
    threads += Thread(None, "another subject"),

    /** Insert dummy posts */
    posts ++= Seq(
      Post(None, 1, Some(java.util.UUID.randomUUID), "one author", "author@one.com", "0110101"),
      Post(None, 1, Some(java.util.UUID.randomUUID), "other author", "author@other.com", "TRIGGERED"),
      Post(None, 2, Some(java.util.UUID.randomUUID), "troll author", "author@troll.lol", "0202202")))

  val setupFuture: Future[Unit] = db.run(setup)

  def listAllThreads = threads.result.statements // threads.result.statements = select * from threads

  def findThreadById(threadId: Long): Future[Option[Thread]] = {
    db.run(this.filter(_.threadId === threadId).result).map(_.headOption)
  }

  def findPostById(postId: Long): Future[Option[Post]] = {
    db.run(posts.filter(_.postId === postId).result).map(_.headOption)
  }

  /**
   *  TODO: add openThread(posts) = open all posts for threadId
   */

  def createThread(t: Thread) = {
    db.run(threads += Thread(None, t.subject))
    // ???    db.run(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = Some(threadId))) += thread)
  }

  def deleteThreadById(threadId: Long): Future[Int] = {
    db.run(this.filter(_.threadId === threadId).delete)
    // ???    threads.filter(t => t.threadId === threadId).delete
  }

  def createPost(p: Post) = {
    db.run(posts += Post(
      None,
      p.threadId,
      Some(java.util.UUID.randomUUID),
      p.pseudonym,
      p.email,
      p.content))
  }

  /** TODO: Probably doesn't work */
  implicit def secretOk(postId: Long, secret: Long): Boolean = findPostById(postId).map(_.map(_.secretId)) == secret

  def editPost(postId: Long, secret: Long, newContent: String) = {
    /** TODO: Add reasonable else instruction on bad secretId */
    val updateContent = posts.filter(_.postId === postId).map(_.content)
    if (secretOk(postId, secret)) db.run(updateContent.update(newContent)) else 0
  }

  def deletePost(postId: Long, secret: Long, post: Post) = {
    /** TODO: Add reasonable else instruction on bad secretId */
    if (secretOk(postId, secret)) db.run(posts.filter(_.postId === postId).delete) else 0

    /**
     * OR:
     * posts.map(p =>
     * Case
     * If (p.secretId === secret) Then DELETE
     * If (p.secretId =!= secret) Then REFUSE))
     */
  }
}