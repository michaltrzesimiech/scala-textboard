package main.scala.textboard

import java.util.UUID
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }

trait DatabaseService {
  import Thread._
  import Post._
  import DAO._

  implicit val db = Database.forConfig("database")
  val ddl = threads.schema ++ posts.schema

  val setup = {
    DBIO.seq(
      /**
       *  Create tables, including primary and foreign keys
       */
      ddl.create,

      /**
       *  Insert dummy threads and posts
       */
      threads += Thread(None, "one subject"),
      threads += Thread(None, "another subject"),
      posts ++= Seq(
        Post(None, 1, Some(java.util.UUID.randomUUID), "one author", "author@one.com", "0110101"),
        Post(None, 1, Some(java.util.UUID.randomUUID), "other author", "author@other.com", "TRIGGERED"),
        Post(None, 2, Some(java.util.UUID.randomUUID), "troll author", "author@troll.lol", "0202202")))
  }

  val setupFuture: Future[Unit] = db.run(setup)

}