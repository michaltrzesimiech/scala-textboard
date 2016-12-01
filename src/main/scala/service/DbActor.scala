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
  case class EditPost(threadId: Long, postId: Long, content: NewContent)
  case class DeletePost(postId: Option[Long])
}

class DbActor extends Actor {
  import DbActor._
  import DAO._

  def receive = {
    case ListAllThreads                         => listAllThreads
    case ListAllThreadsPaginated(limit, offset) => listAllThreadsPaginated(limit, offset)
    case OpenThread(threadId)                   => openThread(threadId)
    case CreateNewThread(thread)                => createNewThread(thread)
    case CreatePost(threadId, post)             => createPost(threadId, post)
    case EditPost(threadId, postId, content)    => editPost(threadId, postId, content)
    case DeletePost(postId)                     => deletePost(postId)
  }
}