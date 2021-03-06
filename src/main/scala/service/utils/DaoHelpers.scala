package textboard.utils

import main.scala.textboard._
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
//import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }

trait DaoHelpers extends DatabaseService {

  import textboard.domain.Post._
  import textboard.domain.Thread._

  def exec[T](action: DBIO[T]): T = {
    Await.result(db.run(action), 2 seconds)
  }

  /**
   * Implicitly turns Int into Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)

  /**
   * Finds last thread ID to add an accompanying post to new thread
   */
  def lastId: Option[Long] = {
    val threadIds = exec(threads.map(_.threadId).result)
    Some(threadIds.max.toLong)

  }
}