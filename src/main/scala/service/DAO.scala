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
  implicit def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret: String = exec(posts.filter(_.id === postId).map(_.secretId).result).head.toString
    postSecret.toString == secret
  }

  /**
   * Needed for creation of new thread along with accompanying post
   */
  def lastId: Option[Long] = {
    val threadIds = exec(threads.map(_.threadId).result)
    Some(threadIds.max.toLong)
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

  def openThread(threadId: Long) = { exec(posts.filter(_.threadId === threadId).result) }

  def editPost(threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    exec(postsContent.update(c.content))
  }

  def deletePost(postId: Option[Long]) = {
    exec(posts.filter(_.id === postId).delete)
  }

  /**
   * NOT REQUIRED:
   * def deleteThreadById(threadId: Long): Future[Int] = {
   * db.run(posts.filter(_.threadId === threadId).delete)
   * db.run(threads.filter(_.threadId === threadId).delete) }
   */
}


