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
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
//import slick.driver.PostgresDriver.backend._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import spray.json._
import spray.json.DefaultJsonProtocol

object DAO extends TableQuery(new Threads(_)) {

  import Thread._
  import Post._

  implicit val db = Database.forConfig("database")

  /**
   * Await db.run(action) 2 seconds
   */
  implicit def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  /**
   *  Implicitly turn Int to Option[Long]
   */
  implicit def adapter(id: Int) = Some(id.toLong)

  val setup = {
    DBIO.seq(
      /**
       *  Create tables, including primary and foreign keys
       */
      (threads.schema ++ posts.schema).create,

      /**
       *  Insert dummy threads and posts
       */
      threads += Thread(None, "one subject"),
      threads += Thread(None, "another subject"),
      posts ++= Seq(
        Post(None, 1, Some(java.util.UUID.randomUUID), "one author", "author@one.com", "0110101"),
        Post(None, 1, Some(java.util.UUID.randomUUID), "other author", "author@other.com", "TRIGGERED"),
        Post(None, 2, Some(java.util.UUID.randomUUID), "troll author", "author@troll.lol", "0202202")))
  }

  val setupFuture: Future[Unit] = db.run(setup)

  /**
   * List all threads
   * - "threads.drop(x).take(y)" is equivalent of SQL: "select * from threads limit y offset x"
   * - "threads.result.statements" is equivalent of SQL: "select * from threads"
   */
  def listAllThreads(offset: Int, limit: Int) = {
    exec(threads.drop(offset).take(limit).result)
  }

  def openThread(threadId: Long) = {
    exec(posts.filter(_.threadId === threadId).result)
  }

  def findThreadById(threadId: Long): Future[Option[Thread]] = {
    db.run(this.filter(_.threadId === threadId).result).map(_.headOption)
  }

  def findPostById(postId: Long): Future[Option[Post]] = {
    db.run(posts.filter(_.postId === postId).result).map(_.headOption)
  }

  def createThread(t: Thread) = {
    exec(threads += Thread(None, t.subject))
    // ???   db.run(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = Some(threadId))) += thread)
  }

  def deleteThreadById(threadId: Long): Future[Int] = {
    db.run(this.filter(_.threadId === threadId).delete)
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

  /**
   *  Verify post secret
   */
  implicit def secretOk(postId: Long, secret: Long): Boolean = {
    val postSecret = exec(posts.filter(_.postId === postId).map(_.secretId).result)
    postSecret == secret
  }

  def editPost(threadId: Long, postId: Long, secret: Long, newContent: String) = {
    val thisPost = posts.filter(_.threadId === threadId)
    val postsContent = thisPost.filter(_.postId === postId).map(_.content)

    if (secretOk(postId, secret)) db.run(postsContent.update(newContent))
    /** TODO: Add reasonable else or refactor to pattern matching */
    else 0
  }

  def deletePost(postId: Long, secret: Long) = {
    if (secretOk(postId, secret)) db.run(posts.filter(_.postId === postId).delete)
    /**
     * TODO:
     * Add reasonable else
     * or: refactor to pattern matching
     * or:
     *         posts.map(p =>
     *             Case
     *                 If (p.secretId === secret) Then DELETE
     *                 If (p.secretId =!= secret) Then REFUSE))
     */
    else 0
  }
}

