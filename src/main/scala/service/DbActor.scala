package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.stream.{ ActorMaterializer, Materializer }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case class ListAllThreads(limit: Int, offset: Int)
  case class OpenThread(threadId: Long)
  case class FindThreadById(thradId: Long)
  case class CreateThread(thread: Thread)
  case class DeleteThreadById(threadId: Long)
  case class CreatePost(post: Post)
  case class EditPost(threadId: Long, postId: Long, secret: Long, newContent: String)
  case class DeletePost(postId: Long, secret: Long)
}

class DbActor extends Actor {
  import DbActor._

  def receive = {
    case ListAllThreads(limit, offset)                  => DAO.listAllThreads(limit, offset)
    case OpenThread(threadId)                           => DAO.openThread(threadId)
    case FindThreadById(threadId)                       => DAO.findThreadById(threadId)
    case CreateThread(thread)                           => DAO.createThread(thread)
    case DeleteThreadById(threadId)                     => DAO.deleteThreadById(threadId)
    case CreatePost(post)                               => DAO.createPost(post)
    /** TODO: rethink and implement */
    case EditPost(threadId, postId, secret, newContent) => DAO.editPost(threadId, postId, secret, newContent)
    case DeletePost(postId, secret)                     => DAO.deletePost(postId, secret)
  }
}

