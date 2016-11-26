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

  /**
   *  Verify post secret
   */
  //  def secretOk(postId: Long, secret: String): Boolean = {
  //    val postSecret = exec(posts.filter(_.id === postId).map(_.secretId).result)
  //    postSecret.toString == secret
  //  }
  def secretOk(postId: Long, secret: String): Boolean = {
    val postSecret = posts.filter(_.id === postId).map(_.secretId).result
    postSecret.toString == secret
  }

  def listAllThreads(offset: Int, limit: Int): Iterable[String] = {
    /**
     * - "threads.drop(x).take(y)" is equivalent of SQL: "select * from threads limit y offset x"
     * - "threads.result.statements" is equivalent of SQL: "select * from threads"
     */
    val sortedThreads = threads.sortBy(_.threadId.desc)
    sortedThreads.drop(offset).take(limit).result.statements
  }

  def justListAllThreads = { threads.result }

  def createThread(t: Thread) = {
    exec(threads += Thread(None, t.subject))
    // ???   db.run(this returning this.map(_.threadId) into ((acc, threadId) => acc.copy(threadId = Some(threadId))) += thread)
  }

  def createPost(threadId: Option[Long], pseudonym: String, email: String, content: String) = {
    exec(posts += Post(None, threadId, secretId, pseudonym, email, content))
  }

  def openThread(threadId: Long) = {
    exec(posts.filter(_.threadId === threadId).result)
  }

  def editPost(threadId: Long, postId: Long, secret: String, newContent: String) = {
    val thisPost = posts.filter(_.threadId === threadId)
    val postsContent = thisPost.filter(_.id === postId).map(_.content)

    if (secretOk(postId, secret))
      db.run(postsContent.update(newContent))
    else
      StatusCodes.Forbidden
  }

  def deletePost(postId: Long, secret: String) = {
    if (secretOk(postId, secret))
      db.run(posts.filter(_.id === postId).delete)
    else
      StatusCodes.Forbidden
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