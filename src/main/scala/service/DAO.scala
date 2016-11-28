package main.scala.textboard

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import java.util.UUID
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import com.wix.accord.dsl._

trait DaoHelpers extends DatabaseService {
  /**
   * Execution helper #1: await database action
   */
  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 3 seconds)

  /**
   * Execution helper #2: implicitly turn Int to expected Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)
}

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers {

  import Thread._
  import Post._
  import WebServer._

  /**
   *  Verify post secret
   */
  def secretOk(postId: Long, secret: String): Boolean = {
    val postSecret = posts.filter(_.id === postId).map(_.secretId)
    val postSecretResult = exec(postSecret.result)
    postSecretResult.head.toString == secret
  }

  def listAllThreads(offset: Int, limit: Int) = {
    val sortedAndPaginated = threads.sortBy(_.threadId.desc).drop(offset).take(limit)
    exec(sortedAndPaginated.to[Seq].result)

    log.info(sortedAndPaginated.result.statements.toString)
  }

  def justListAllThreads = {
    exec(threads.to[Seq].result)

    // def justListAllThreads = {
    // val allThreads = threads.map(t => (t.threadId, t.subject))
    // db.run(allThreads.to[Seq].result) }
  }

  def openThread(threadId: Long) = {
    val postsByThreadId = posts.filter(_.threadId === threadId).sortBy(p => p.id.asc)
    exec(postsByThreadId.to[Seq].result)
  }

  def createNewThread(subject: String, pseudonym: String, email: String, content: String) = {
    createThread(subject)
    // ^== exec(threads += Thread(None, subject))

    val ids = threads.map(_.threadId).result
    val lastId: Long = exec(for (id <- ids) yield id.last.toLong /* + 1*/ )

    createPost(Some(lastId), pseudonym, email, content)
    // ^== exec(posts += Post(None, Some(lastId), secretId, pseudonym, email, content))
  }

  def createThread(subject: String) = {
    exec(threads += Thread(None, subject))
    // exec(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = Some(threadId))) += Thread(threadId, subject))
  }

  def createPost(threadId: Option[Long], pseudonym: String, email: String, content: String) = {
    exec(posts += Post(None, threadId, secretId, pseudonym, email, content))
  }

  def editPost(postId: Long, secret: String, newContent: String) = {
    val thisPostsContent = posts.filter(_.id === postId).map(_.content)
    val updateContent = if (secretOk(postId, secret)) exec(thisPostsContent.update(newContent))
    else StatusCodes.Forbidden
  }

  def deletePost(postId: Long, secret: String) = {
    if (secretOk(postId, secret))
      exec(posts.filter(_.id === postId).delete)
    else StatusCodes.Forbidden
  }

  /**
   *  NOT REQUESTED OR NOT REQUIRED:
   * def deleteThreadById(threadId: Long): Future[Int] = {
   * db.run(posts.filter(_.threadId === threadId).delete)
   * db.run(threads.filter(_.threadId === threadId).delete)
   * }
   *
   * def findThreadById(threadId: Long): Future[Option[Thread]] = {
   * db.run(this.filter(_.threadId === threadId).result).map(_.headOption)
   * }
   *
   * def findPostById(postId: Long): Future[Option[Post]] = {
   * db.run(posts.filter(_.id === postId).result).map(_.headOption)
   * }
   */
}