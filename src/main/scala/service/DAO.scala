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

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers with ConfigHelper {

  import Thread._
  import Post._

  /** TODO: sort by last posted in, use timestamps and ordering */
  def listAllThreadsPaginated(limit: Int, offset: Int) = {

    //    implicit def ordering: Ordering[DateTime] = Ordering.by(_.getMillis)
    //    ordering.reverse

    exec(threads.sortBy(_.threadId.desc).drop(limit).take(offset).result)

    //    exec(threads.sortBy(_.threadId.asc).drop(dbOffset).take(dbLimit).result)
  }

  def openThread(threadId: Long, limit: Int, offset: Int) = {
    val rawPosts = posts.filter(_.threadId === threadId)
    val sortedPaginatedContents = rawPosts
      .sortBy(_.id.asc).drop(limit).take(offset)
      .map(x => (x.id, x.pseudonym, x.email, x.content))

    exec(sortedPaginatedContents.result)
  }

  /** TODO: apply working solution for returning secret */
  def createNewThread(nt: NewThread) = {
    exec(threads += Thread(None, nt.subject /*, DateTime.now*/ ))
    exec(posts += Post(None, lastId, secretId, nt.pseudonym, nt.email, nt.content /*, DateTime.now*/ ))
  }

  /** TODO: test for returned token */
  def createPost(threadId: Option[Long], p: Post) = {
    exec((posts returning posts.map(_.secretId))
      += Post(None, p.threadId, secretId, p.pseudonym, p.email, p.content /*, DateTime.now*/ ))

    /*   add modification to thread lastmodified */
  }

  def editPost(threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    exec(postsContent.update(c.content))
  }

  def deletePost(postId: Option[Long]) = {
    exec(posts.filter(_.id === postId).delete)
  }

  /**
   * Verifies post secret while updating or deleting posts
   */
  implicit def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret: String = exec(posts.filter(_.id === postId).map(_.secretId).result).head.toString
    postSecret.toString == secret
  }

  implicit def maxLimit(limit: Int) = {
    if (limit > dbLimit || limit <= 0) limit.map(x => dbLimit)
  }
}

/** 
 * = roll out supported timestamp format, add filtering threads by latest answered post
 * ? update sample jsons in /json
 * ? clean up gitignores
 */
