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

  import Thread._
  import Post._

  /**
   * Execution helper
   */
  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  /**
   * Implicitly turn Int to expected Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)

  /**
   * Verifies post secret (needed for updating or deleting posts)
   */
  def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret = exec(posts.filter(_.id === postId).map(_.secretId).result)
    postSecret.toString == secret
  }

  /**
   * Needed for creation of new thread along with accompanying post
   */
  def lastId: Option[Long] = {
    val threadIds = threads.map(_.threadId).result
    exec(for (id <- threadIds) yield Some(id.last.toLong))
  }
}

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers {

  import Thread._
  import Post._

  def listAllThreads = exec(threads.result)

  def listAllThreadsPaginated(offset: Int, limit: Int) = {
    exec(threads.sortBy(_.threadId.desc).drop(offset).take(limit).result)
  }

  def createNewThread(nt: NewThread) = {
    exec(threads += Thread(None, nt.subject))
    exec(posts += Post(None, lastId, secretId, nt.pseudonym, nt.email, nt.content))
  }

  def createPost(threadId: Option[Long], p: Post) = {
    exec(posts += Post(None, p.threadId, secretId, p.pseudonym, p.email, p.content))
  }

  def openThread(threadId: Long) = {
    exec(posts.filter(_.threadId === threadId).result)
  }

  def editPost(secret: String, threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    secretOk(Option(postId), secret) match {
      case true  => exec(postsContent.update(c.content))
      case false => StatusCodes.Forbidden
    }
    // if (secretOk(p.postId, secret)) exec(postsContent.update(p.content)) else StatusCodes.Forbidden
    /**
     * def editPost(secret: String, p: Post) = {
     * val postsContent = posts.filter(x => x.threadId === p.threadId && x.id === p.postId).map(_.content)
     * secretOk(p.postId, secret) match {
     * case true => exec(postsContent.update(p.content))
     * case false => StatusCodes.Forbidden
     * }
     * // if (secretOk(p.postId, secret)) exec(postsContent.update(p.content)) else StatusCodes.Forbidden
     * }
     */
  }

  def deletePost(secret: String, postId: Option[Long]) = {
    secretOk(postId, secret) match {
      case true  => exec(posts.filter(_.id === postId).delete)
      case false => StatusCodes.Forbidden
    }
    // if (secretOk(postId, secret)) exec(posts.filter(_.id === postId).delete) else StatusCodes.Forbidden
  }

  /**
   * NOT REQUIRED:
   * def deleteThreadById(threadId: Long): Future[Int] = {
   * db.run(posts.filter(_.threadId === threadId).delete)
   * db.run(threads.filter(_.threadId === threadId).delete) }
   *
   * def findThreadById(threadId: Long): Future[Option[Thread]] = {
   * db.run(this.filter(_.threadId === threadId).result).map(_.headOption) }
   *
   * def findPostById(postId: Long): Future[Option[Post]] = {
   * db.run(posts.filter(_.id === postId).result).map(_.headOption) }
   */
}
