package textboard.utils

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import com.typesafe.config.ConfigFactory
import java.util.UUID
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import textboard._

trait DaoHelpers extends DatabaseService {

  import textboard.domain._

  val threads = Thread.threads
  val posts = Post.posts

  /**
   * Helps execution of db.run()
   */
  def exec[T](action: DBIO[T]): T =
    Await.result(db.run(action), 2 seconds)

  /**
   * Implicitly turns Int into Option[Long]
   */
  implicit def intToOptionLong(id: Int) = Some(id.toLong)

  /**
   * Generates and assigns secret ID
   */
  implicit val secretId: String = UUID.randomUUID.toString()

  /**
   * Finds last thread ID to add an accompanying post to new thread
   */
  def lastId: Option[Long] = {
    val threadIds = exec(threads.map(_.threadId).result)
    Some(threadIds.max.toLong)
  }
}