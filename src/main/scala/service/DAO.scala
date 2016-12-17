package main.scala.textboard

import akka.http.scaladsl.model._
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import scala.concurrent.{ Await, ExecutionContextExecutor }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.util._
import textboard.utils._
import textboard.domain._
import java.util.UUID

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers with ConfigHelper {

  import Thread._
  import Post._

  /** TODO: Potentially extract */
  implicit def ordering: Ordering[DateTime] = Ordering.by(_.getMillis)
  ordering.reverse

  def listAllThreadsPaginated(limit: Int, offset: Int) = {
    /** TODO: Either join with Post or introduce lastRepliedTo */
    exec(threads.sortBy(_.threadId.desc).drop(limit).take(offset).result)
  }

  def openThread(threadId: Long, limit: Int, offset: Int) = {
    val rawPosts = posts.filter(_.threadId === threadId)
    val sortedPaginatedContents = rawPosts
      .sortBy(_.id.asc).drop(limit).take(offset)
      .map(x => (x.id, x.pseudonym, x.email, x.content))

    exec(sortedPaginatedContents.result)
  }

  def createNewThread(nt: NewThread) = {
    val secretId: String = UUID.randomUUID.toString
    exec(threads += Thread(None, nt.subject /*, DateTime.now*/ ))
    exec(posts += Post(None, lastId, secretId, nt.pseudonym, nt.email, nt.content, DateTime.now))
  }

  /**
   * TODO: Parsing timestamp from JSON doesn't work for this method.
   */
  def createPost(threadId: Long, p: Post) = {
    val secretId: String = UUID.randomUUID.toString
    exec(posts returning posts.map(_.secretId)
      += Post(None, p.threadId, secretId, p.pseudonym, p.email, p.content, DateTime.now))

    /** TODO: add update to thread.lastRepliedTo if exists */
  }

  def editPost(threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    exec(postsContent.update(c.content))
  }

  def deletePost(postId: Option[Long]) = {
    exec(posts.filter(_.id === postId).delete)
  }

  /**
   * Implicitly verifies post secret while updating or deleting posts
   */
  implicit def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret: String = exec(posts.filter(_.id === postId).map(_.secretId).result).head.toString
    postSecret.toString == secret
  }

  /**
   *  Implicitly applies global limit configured in application.config
   */
  implicit def maxLimit(limit: Int) = {
    if (limit > dbLimit || limit <= 0) limit.map(x => dbLimit)
  }
}

/** 
 * TODO:
 * = Add filtering threads by last answered
 * = Fix support for limit and offset
 * - update jsons
 */
