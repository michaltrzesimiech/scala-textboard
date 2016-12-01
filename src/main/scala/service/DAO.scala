package main.scala.textboard

import akka.http.scaladsl.model._
import java.util.UUID
import scala.concurrent.{ Await, ExecutionContextExecutor }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.util._

trait DaoHelpers extends DatabaseService {

  import Thread._
  import Post._

  /**
   * Helps execution of db.run()
   */
  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  /**
   * Implicitly turns Int into Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)

  /**
   * Generates and assigns secret ID
   */
  implicit def secretId: String = UUID.randomUUID.toString()

  /**
   * Verifies post secret while updating or deleting posts
   */
  def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret: String = exec(posts.filter(_.id === postId).map(_.secretId).result).head.toString
    postSecret.toString == secret
  }

  /**
   * Finds last thread ID to add an accompanying post to new thread
   */
  def lastId: Option[Long] = {
    val threadIds = exec(threads.map(_.threadId).result)
    Some(threadIds.max.toLong)
  }
}

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers {

  import Thread._
  import Post._

  def listAllThreadsPaginated(offset: Int, limit: Int) = {
    exec(threads.sortBy(_.threadId.desc).drop(offset).take(limit).result)
  }

  def openThread(threadId: Long) = {
    exec(posts.filter(_.threadId === threadId).result)
  }

  def createNewThread(nt: NewThread) = {
    exec(threads += Thread(None, nt.subject))
    exec(posts += Post(None, lastId, secretId, nt.pseudonym, nt.email, nt.content))
  }

  def createPost(threadId: Option[Long], p: Post) = {
    exec(posts += Post(None, p.threadId, secretId, p.pseudonym, p.email, p.content))
  }

  def editPost(threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    exec(postsContent.update(c.content))
  }

  def deletePost(postId: Option[Long]) = {
    exec(posts.filter(_.id === postId).delete)
  }
}
