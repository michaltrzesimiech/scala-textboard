package main.scala.textboard

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
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
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }

/**
 *  Threads table
 *  @param threadId Auto-incremented primary key column holding unique ID for Thread.
 */
final class Threads(tag: Tag) extends Table[Thread](tag, "THREADS") {
  /** Auto Increment the threadId primary key column */
  def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
  def subject = column[String]("SUBJECT")

  def * : ProvenShape[Thread] = (threadId.?, subject) <> ((Thread.apply _).tupled, Thread.unapply)
}

/**
 *  Posts table
 *  @param postId Auto-incremented primary key column holding unique ID for Post.
 */
final class Posts(tag: Tag) extends Table[Post](tag, "POSTS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def threadId = column[Long]("THREAD_ID")
  def secretId = column[String]("SECRET")
  def pseudonym = column[String]("PSEUDONYM")
  def email = column[String]("EMAIL")
  def content = column[String]("CONTENT")

  def * : ProvenShape[Post] = (
    id.?,
    threadId.?,
    secretId,
    pseudonym,
    email,
    content) <> ((Post.apply _).tupled, Post.unapply)

  /**
   *  A reified foreign key relation that can be navigated to create a join
   */
  def thread = foreignKey("THR_FK", threadId, Thread.threads)(_.threadId)
}

/**
 * Domain model for Thread
 */
case class Thread(
  /** Auto-incremented columns are automatically ignored. */
  threadId: Option[Long] = None,
  subject: String)

/**
 * Domain model for Post
 */
case class Post(
  postId: Option[Long] = None,
  threadId: Option[Long],
  secretId: String,
  pseudonym: String,
  email: String,
  content: String)

object Thread {
  /**
   *  Query interface for the Threads table
   */
  val threads: TableQuery[Threads] = TableQuery[Threads]
}

object Post {
  /**
   *  Query interface for the Posts table
   */
  val posts: TableQuery[Posts] = TableQuery[Posts]
}