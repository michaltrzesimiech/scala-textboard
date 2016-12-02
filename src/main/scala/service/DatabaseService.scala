package textboard

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import textboard.utils._
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import slick.util._

trait DatabaseService extends ConfigHelper with DateTimeHelper {

  import textboard.domain._
  import DAO._
  
  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)

  private val dataSource = new HikariDataSource(hikariConfig)
  val driver = slick.driver.PostgresDriver
  val db = Database.forDataSource(dataSource)

  db.createSession()

  lazy val ddl = threads.schema ++ posts.schema
  lazy val initSetup = {
    DBIO.seq(
      /**
       *  Creates tables, including primary and foreign keys
       */
      ddl.create,

      /**
       *  Inserts dummy threads and posts
       */
      threads += Thread(None, "SUBJECT A"),
      threads += Thread(None, "SUBJECT B"),
      posts ++= Seq(
        Post(None, 1, secretId, "Agent A", "agent@one.com", "COMMENT", now),
        Post(None, 1, secretId, "Agent B", "author@other.com", "COMMENT", now),
        Post(None, 2, secretId, "Agent C", "author@troll.lol", "COMMENT", now)))
  }

  /**
   *  Run initial setup if no prior setup is found
   *  TODO: compare val futureInitialSetup: Future[Unit] = db.run(initSetup)
   */
  try {
    def createTablesIfNotInTables(tables: Vector[MTable]) = {
      if (!tables.exists(_.name.name == threads.baseTableRow.tableName)) {
        db.run(initSetup)
      } else {
        Future()
      }
    }
    val createTablesIfNone = db.run(MTable.getTables).flatMap(createTablesIfNotInTables)
    Await.result(createTablesIfNone, Duration.Inf)
  } finally db.close
}
