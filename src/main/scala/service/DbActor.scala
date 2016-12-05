package main.scala.textboard

import akka.actor.Actor
import textboard.domain._

object DbActor {
  case class CreateNewThread(thread: NewThread)
}

class DbActor extends Actor {
  import DbActor._
  import DAO._

  def receive = {
    case CreateNewThread(thread)      => createNewThread(thread)
  }
}