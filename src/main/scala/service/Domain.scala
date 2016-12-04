//package main.scala.textboard
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import slick._
//import slick.util._
//import slick.driver.PostgresDriver.api._
//import slick.lifted.{ AbstractTable, Rep, ProvenShape }
//
///**
// *  Threads table
// *  @param threadId Auto-incremented primary key column holding unique ID for Thread.
// */
//final class Threads(tag: Tag) extends Table[Thread](tag, "THREADS") {
//  /** Auto Increment the threadId primary key column */
//  def threadId = column[Long]("THREAD_ID", O.PrimaryKey, O.AutoInc)
//  def subject = column[String]("SUBJECT")
//
//  def * : ProvenShape[Thread] = (threadId.?, subject) <> ((Thread.apply _).tupled, Thread.unapply)
//}
//
///**
// *  Posts table
// *  @param postId Auto-incremented primary key column holding unique ID for Post.
// */
//final class Posts(tag: Tag) extends Table[Post](tag, "POSTS") {
//  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
//  def threadId = column[Long]("THREAD_ID")
//  def secretId = column[String]("SECRET")
//  def pseudonym = column[String]("PSEUDONYM")
//  def email = column[String]("EMAIL")
//  def content = column[String]("CONTENT")
//
//  def * : ProvenShape[Post] = (
//    id.?,
//    threadId.?,
//    secretId,
//    pseudonym,
//    email,
//    content) <> ((Post.apply _).tupled, Post.unapply)
//
//  /**
//   *  A reified foreign key relation that can be navigated to create a join
//   */
//  def thread = foreignKey("THR_FK", threadId, Thread.threads)(_.threadId)
//}
//
///**
// * Domain model for Thread
// */
//case class Thread(threadId: Option[Long] = None, subject: String) {
//  require(!subject.isEmpty, "Subject must not be empty")
//}
//
///**
// * Domain model for Post
// */
//case class Post(
//    postId: Option[Long] = None,
//    threadId: Option[Long],
//    secretId: String,
//    pseudonym: String,
//    email: String,
//    content: String) {
//  require((!pseudonym.isEmpty && pseudonym.length < 12), "Pseudonym must be between 0 and 12 characters")
//  require(!email.isEmpty, "Email must not be empty")
//  require((email.contains("@") && email.contains(".") && email.length < 30), "Email doesn't look proper.")
//  require(!content.isEmpty, "Content must not be empty")
//}
//
//case class NewThread(
//    postId: Option[Long] = None,
//    subject: String,
//    secretId: String,
//    pseudonym: String,
//    email: String,
//    content: String) {
//  require(!subject.isEmpty, "Subject must not be empty")
//  require((!pseudonym.isEmpty && pseudonym.length < 12), "Pseudonym must be between 0 and 12 characters")
//  require(!email.isEmpty, "Email must not be empty")
//  require((email.contains("@") && email.contains(".") && email.length < 30), "Email doesn't look proper.")
//  require(!content.isEmpty, "Content must not be empty")
//}
//
//case class NewContent(content: String)
//
//object Thread {
//  /**
//   *  Query interface for the Threads table
//   */
//  val threads: TableQuery[Threads] = TableQuery[Threads]
//}
//
//object Post {
//  /**
//   *  Query interface for the Posts table
//   */
//  val posts: TableQuery[Posts] = TableQuery[Posts]
//}
//
//object NewThread
//
//object NewContent
