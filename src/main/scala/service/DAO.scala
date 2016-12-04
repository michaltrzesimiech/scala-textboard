package textboard

import akka.http.scaladsl.model._
import textboard.utils._
import textboard.domain._
import java.util.UUID
import scala.concurrent.{ Await, ExecutionContextExecutor }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.util._

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import com.github.tototoshi.slick.H2JodaSupport._
import org.joda.time.DateTime

object DAO extends TableQuery(new Threads(_)) with DatabaseService with DaoHelpers {

  import textboard.domain._
  import Thread._
  import Post._
  import textboard.utils._

  def listAllThreadsPaginated(limit: Int, offset: Int) = {

    /** TODO: Sort threads by last answered (foreign key?) */
//    val bareSortedPosts = posts.sortBy(_.timestamp.desc)
//
//    val join = for {
//      (t, p) <- threads join posts
//    } yield ((t.threadId -> t.subject), (p.timestamp))
//    val joinSorted = join.sortBy(_._2.desc)
//
//    exec(joinSorted.map(_._1).result)

     exec(threads.sortBy(_.threadId.desc).drop(limit).take(offset).result)
  }

  def openThread(threadId: Long, limit: Int, offset: Int) = {
    /** TODO: Check with requirements */
    exec(posts.sortBy(_.id.asc).filter(_.threadId === threadId).drop(limit).take(offset).result)
  }

  def createNewThread(nt: NewThread) = {
    exec(threads += Thread(None, nt.subject))
    println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + DateTime.now)
    exec(posts += Post(None, lastId, secretId, nt.pseudonym, nt.email, nt.content, DateTime.now))
  }

  def createPost(threadId: Option[Long], p: Post) = {
    exec(posts += Post(None, p.threadId, secretId, p.pseudonym, p.email, p.content, DateTime.now))
  }

  def editPost(threadId: Long, postId: Long, c: NewContent) = {
    val postsContent = posts.filter(x => x.threadId === threadId && x.id === postId).map(_.content)
    exec(postsContent.update(c.content))
  }

  def deletePost(postId: Option[Long]) = {
    exec(posts.filter(_.id === postId).delete)
  }

  /**
   * Verifies post secret. Needed for updating or deleting posts.
   */
  def secretOk(postId: Option[Long], secret: String): Boolean = {
    val postSecret: String = exec(posts.filter(_.id === postId).map(_.secretId).result).head.toString
    postSecret.toString == secret
  }
}

/**
 * TODO: Clean up and finalize
 * t ensure proper display
 * t change hardcoded limit to max limit
 * 
 * x mechanism to display secret on create
	
 * v custom format for datetime, saving datetime to db, writing back out
 * v add display limits to opened thread
 * v potentially restructure; domain to /domain/{a, b, c}
 * v extract /services/{validation, database}
 * v calibrate limit and offset
 * 
 * ? add rule to new post: if thread has to exist for new posts
 * ? update sample jsons in /json
 * ? clean up gitignores
 * ? 	add "joda-time" % "joda-time" % "2.9.6", 
 *   	"com.github.tototoshi" % "slick-joda-mapper_2.11" % "2.2.0",
 * 		"org.joda" % "joda-convert" % "1.8.1"
 */

/** 
 *  TESTED
 * v automate DB initialization based on predicate = schema exists
 *  
 *  
 */
