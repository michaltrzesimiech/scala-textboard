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
import textboard.utils._
import textboard.domain._

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers with ConfigHelper {

  import Thread._
  import Post._

  def listAllThreadsPaginated(limit: Int, offset: Int) = {
    exec(threads.sortBy(_.threadId.desc).drop(dbOffset).take(dbLimit).result)

    //    val bareSortedPosts = posts.sortBy(_.timestamp.desc)
    //    val join = for {
    //      (t, p) <- threads join posts
    //    } yield ((t.threadId -> t.subject), (p.timestamp))
    //    val joinSorted = join.sortBy(_._2.desc)
    //    exec(joinSorted.map(_._1).result)
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
    println(posts returning posts.map(_.secretId))
    println(p.secretId)
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

  //  def displaySecret(postId: Option[Long]) = {
  //    val secretAssigned = (posts.filter(_.id === postId).map(_.secretId))
  //    println(secretAssigned)
  //    exec(secretAssigned.result.head)
  //  }
}

/** 
 * = display secret on create
 * = add supported timestamp format, add filtering threads by latest answered post
 * = set max display limit defined from config
 * == fix custom parameters for limit and offset
 * == add custom pagination for posts
 * == add display limits to opened thread
 * 
 * === update sample jsons in /json
 * === clean up gitignores
 * 
 * ? potentially add rule to new post: if thread has to exist for new posts
 * ? potentially restructure; domain to /domain/{a, b, c}
 * ? extract /services/{validation, database}
 */
