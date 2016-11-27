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

trait DaoHelpers extends DatabaseService {
  /**
   * Execution helper #1: await database action
   */
  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

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
    val postSecretQuery = posts.filter(_.id === postId).map(_.secretId)
    val postSecret = exec(postSecretQuery.result)
    log.info(postSecretQuery.result.statements.toString)
    postSecret.toString == secret
  }

  def listAllThreads(offset: Int, limit: Int) = {
    val sortedAndPaginated = threads.sortBy(_.threadId.desc).drop(offset).take(limit)
    log.info(sortedAndPaginated.result.statements.toString)
    exec(sortedAndPaginated.result)
  }

  def justListAllThreads = {
    val allThreads = threads.map(t => (t.threadId, t.subject))
    log.info(allThreads.result.statements.toString)
    exec(allThreads.result)
  }

  def openThread(threadId: Long) = {
    exec(posts.filter(_.threadId === threadId).result)
  }

  def createThread(threadId: Option[Long], subject: String) = {
    // val threadIds = threads.map(_.threadId).max
    // val maxThreadId = threadIds.result

    exec(threads += Thread(threadId, subject))
    // exec(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = Some(threadId))) += Thread(threadId, subject))
  }

  def createPost(threadId: Option[Long], pseudonym: String, email: String, content: String) = {
    exec(posts += Post(None, threadId, secretId, pseudonym, email, content))
  }

  def editPost(postId: Long, secret: String, newContent: String) = {
    val thisPostsContent = posts.filter(_.id === postId).map(_.content)
    val updateContent = if (secretOk(postId, secret)) exec(thisPostsContent.update(newContent))
    else StatusCodes.Forbidden

    log.debug("updating")
  }

  def deletePost(postId: Long, secret: String) = {
    if (secretOk(postId, secret))
      db.run(posts.filter(_.id === postId).delete)
    else StatusCodes.Forbidden
  }

  //  def deleteThreadById(threadId: Long): Future[Int] = {
  //    db.run(posts.filter(_.threadId === threadId).delete)
  //    db.run(threads.filter(_.threadId === threadId).delete)
  //  }

  //  def findThreadById(threadId: Long): Future[Option[Thread]] = {
  //    db.run(this.filter(_.threadId === threadId).result).map(_.headOption)
  //  }

  //  def findPostById(postId: Long): Future[Option[Post]] = {
  //    db.run(posts.filter(_.id === postId).result).map(_.headOption)
  //  }

}