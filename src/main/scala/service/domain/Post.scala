package textboard.domain

import java.sql.Timestamp
import main.scala.textboard._
//import org.joda.time.DateTime
//import org.joda.time.DateTimeZone.UTC
import scala.concurrent.ExecutionContext.Implicits.global
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape }
import textboard.utils._

/**
 *  Posts table
 */
final class Posts(tag: Tag) extends Table[Post](tag, "POSTS") /*with CustomColumnTypes*/ {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def threadId = column[Long]("THREAD_ID")
  def secretId = column[String]("SECRET")
  def pseudonym = column[String]("PSEUDONYM")
  def email = column[String]("EMAIL")
  def content = column[String]("CONTENT")
  /*def timestamp = column[DateTime]("TIMESTAMP")*/

  def * : ProvenShape[Post] = (
    id.?,
    threadId.?,
    secretId,
    pseudonym,
    email,
    content /*,
    timestamp*/ ) <> ((Post.apply _).tupled, Post.unapply)

  /**
   *  A reified foreign key relation that can be navigated to create a join
   */
  //  def thread = foreignKey("THREAD_FK", threadId, Thread.threads)(_.threadId)
}

/**
 * Domain model for Post
 */
case class Post(
    postId: Option[Long] = None,
    threadId: Option[Long],
    secretId: String,
    pseudonym: String,
    email: String,
    content: String /*,
    timestamp: DateTime*/ ) {
  require((!pseudonym.isEmpty && pseudonym.length < 12), "Pseudonym must be between 0 and 12 characters")
  require(!email.isEmpty, "Email must not be empty")
  require((email.contains("@") && email.contains(".") && email.length < 30), "Email doesn't look properly formatted.")
  require(!content.isEmpty, "Content must not be empty")
  require(!threadId.isEmpty, "Posts have to have assigned threads.")
}

object Post {
  /**
   *  Query interface for the Posts table
   */
  val posts: TableQuery[Posts] = TableQuery[Posts]
}

case class NewContent(content: String)

object NewContent