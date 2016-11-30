package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.stream.{ ActorMaterializer, Materializer }
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case object ListAllThreads
  case class ListAllThreadsPaginated(limit: Int, offset: Int)
  case class OpenThread(threadId: Long)
  case class CreateNewThread(thread: NewThread)
  case class CreatePost(threadId: Option[Long], post: Post)
  //  case class EditPost(secret: String, post: Post)
  case class EditContent(secret: String, threadId: Long, postId: Long, content: NewContent)
  case class DeletePost(secret: String, postId: Option[Long])
}

class DbActor extends Actor {
  import DbActor._

  def receive = {
    case ListAllThreads                                 => DAO.listAllThreads
    case ListAllThreadsPaginated(limit, offset)         => DAO.listAllThreadsPaginated(limit, offset)
    case OpenThread(threadId)                           => DAO.openThread(threadId)
    case CreateNewThread(thread)                        => DAO.createNewThread(thread)
    case CreatePost(threadId, post)                     => DAO.createPost(threadId, post)
    // case EditPost(secret, post) => DAO.editPost(secret, post)
    case EditContent(secret, threadId, postId, content) => DAO.editPost(secret, threadId, postId, content)
    case DeletePost(secret, postId)                     => DAO.deletePost(secret, postId)
  }
}
