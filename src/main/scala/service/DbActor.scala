package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.stream.{ ActorMaterializer, Materializer }
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case class ListAllThreads(limit: Int, offset: Int)
  case class OpenThread(threadId: Long)
  case class CreateThread(thread: Thread)
  case class CreatePost(threadId: Option[Long], pseudonym: String, email: String, content: String)
  case class EditPost(threadId: Long, postId: Long, secret: String, newContent: String)
  case class DeletePost(postId: Long, secret: String)
  //  case object SimplyListAllThreads
  //  case class FindThreadById(thradId: Long)
  //  case class DeleteThreadById(threadId: Long)
}

class DbActor extends Actor {
  import DbActor._

  def receive = {
    case ListAllThreads(limit, offset)                   => DAO.listAllThreads(limit, offset)
    case OpenThread(threadId)                            => DAO.openThread(threadId)
    case CreateThread(thread)                            => DAO.createThread(thread)
    case CreatePost(threadId, pseudonym, email, content) => DAO.createPost(threadId, pseudonym, email, content)
    case EditPost(threadId, postId, secret, newContent)  => DAO.editPost(threadId, postId, secret, newContent)
    case DeletePost(postId, secret)                      => DAO.deletePost(postId, secret)
    // case SimplyListAllThreads                         => DAO.justListAllThreads
    // case FindThreadById(threadId)                     => DAO.findThreadById(threadId)
    // case DeleteThreadById(threadId)                   => DAO.deleteThreadById(threadId)
  }
}

