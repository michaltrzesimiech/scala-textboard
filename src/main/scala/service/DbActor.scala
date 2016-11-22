package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.stream.{ ActorMaterializer, Materializer }
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global

object DbActor {
  case object ListAllThreads
  case class FindThreadById(thradId: Long)
  case class CreateThread(thread: Thread)
  case class DeleteThreadById(threadId: Long)
  case class CreatePost(post: Post)
  //  case class EditPost(postId, secret, post)
  //  case class DeletePost(postId, secret, post)
}

class DbActor extends Actor {
  import DbActor._

  def receive = {
    case ListAllThreads             => DAO.listAllThreads
    case FindThreadById(threadId)   => DAO.findThreadById(threadId)
    case CreateThread(thread)       => DAO.createThread(thread)
    case DeleteThreadById(threadId) => DAO.deleteThreadById(threadId)
    case CreatePost(post)           => DAO.createPost(post)
    //    case EditPost(postId, secret, post) => DAO.editPost(postId, secret, post)
    //    case DeletePost(postId, secret, post) => DAO.deletePost(postId, secret, post)
  }
}

