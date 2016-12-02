package textboard.domain

import scala.concurrent.ExecutionContext.Implicits.global
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape }

import textboard.domain._

/**
 *  Threads table
 *  @param threadId Auto-incremented primary key column holding unique ID for Thread.
 */
final class Threads(tag: Tag) extends Table[Thread](tag, "THREADS") {
  /** Auto Increment the threadId primary key column */
  def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
  def subject = column[String]("SUBJECT")

  def * : ProvenShape[Thread] = (threadId.?, subject) <> ((Thread.apply _).tupled, Thread.unapply)

  /**
   *  A reified foreign key relation that can be navigated to create a join
   */
//  def post = foreignKey("TIMESTAMP_FK", timestamp, Post.posts)(_.timestamp)
}

/**
 * Domain model for Thread
 */
case class Thread(threadId: Option[Long] = None, subject: String) {
  require(!subject.isEmpty, "Subject must not be empty")
}

case class NewThread(
    postId: Option[Long] = None,
    subject: String,
    secretId: String,
    pseudonym: String,
    email: String,
    content: String) {
  require(!subject.isEmpty, "Subject must not be empty")
  require((!pseudonym.isEmpty && pseudonym.length < 12), "Pseudonym must be between 0 and 12 characters")
  require(!email.isEmpty, "Email must not be empty")
  require((email.contains("@") && email.contains(".") && email.length < 30), "Email doesn't look proper.")
  require(!content.isEmpty, "Content must not be empty")
}

object Thread {
  /**
   *  Query interface for the Threads table
   */
  val threads: TableQuery[Threads] = TableQuery[Threads]
}

object NewThread
