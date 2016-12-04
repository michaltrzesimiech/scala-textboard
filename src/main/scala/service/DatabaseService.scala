package textboard

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import textboard.utils._
import textboard.domain._
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

  import Thread._
  import Post._

  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)

  private val dataSource = new HikariDataSource(hikariConfig)
  val driver = slick.driver.PostgresDriver
  val db = Database.forDataSource(dataSource)

  db.createSession()

  val ddl = threads.schema ++ posts.schema

  /**
   *  Run schema setup if no setup is found
   */
  try {
    def createTablesIfNone(tables: Vector[MTable]) = {
      if (!tables.exists(_.name.name == threads.baseTableRow.tableName)) {
        db.run(ddl.create)
      } else {
        Future.successful("Initial database setup already in place.")
      }
    }
    val initialSetup = db.run(MTable.getTables).flatMap(createTablesIfNone)
    Await.result(initialSetup, Duration.Inf)
  } finally db.close
}
