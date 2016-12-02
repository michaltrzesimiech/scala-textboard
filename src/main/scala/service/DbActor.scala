package textboard

import akka.actor.Actor
import textboard.domain._

object DbActor {
  case class CreateNewThread(thread: NewThread)
  case class CreatePost(threadId: Option[Long], post: Post)
}

class DbActor extends Actor {
  import DbActor._
  import DAO._

  def receive = {
    case CreateNewThread(thread) => createNewThread(thread)
    case CreatePost(threadId, post) => createPost(threadId, post)
  }
}