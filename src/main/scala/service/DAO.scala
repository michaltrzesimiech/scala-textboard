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

object DAO extends TableQuery(new Threads(_)) with DatabaseService {

  import Thread._
  import Post._

  /**
   * Execution helper #1: await database action
   */
  implicit def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  /**
   * Execution helper #2: implicitly turn Int to expected Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)

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

  def createPost(threadId: Option[Long], pseudonym: String, email: String, content: String) = {
    db.run(posts += Post(
      None,
      threadId,
      Some(java.util.UUID.randomUUID),
      pseudonym,
      email,
      content))
  }

  /**
   *  Verify post secret
   */
  implicit def secretOk(postId: Long, secret: String): Boolean = {
    val postSecret = exec(posts.filter(_.postId === postId).map(_.secretId).result)
    postSecret.toString == secret
  }

  def editPost(threadId: Long, postId: Long, secret: String, newContent: String) = {
    val thisPost = posts.filter(_.threadId === threadId)
    val postsContent = thisPost.filter(_.postId === postId).map(_.content)

    if (secretOk(postId, secret))
      db.run(postsContent.update(newContent))
    else
      StatusCodes.Forbidden
  }

  def deletePost(postId: Long, secret: String) = {
    /**
     * or: refactor to pattern matching
     * or:
     *         posts.map(p =>
     *             Case
     *                 If (p.secretId === secret) Then DELETE
     *                 If (p.secretId =!= secret) Then REFUSE))
     */
    if (secretOk(postId, secret))
      db.run(posts.filter(_.postId === postId).delete)
    else
      StatusCodes.Forbidden
  }

  //  def deleteThreadById(threadId: Long): Future[Int] = {
  //    db.run(posts.filter(_.threadId === threadId).delete)
  //    db.run(threads.filter(_.threadId === threadId).delete)
  //  }
}