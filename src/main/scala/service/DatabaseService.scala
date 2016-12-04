package main.scala.textboard

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.jdbc.meta.MTable
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import slick.util._
import textboard.domain._
import textboard.utils._

//trait ConfigHelper {
//  private val config = ConfigFactory.load()
//
//  private val httpConfig = config.getConfig("http")
//  private val dbConfig = config.getConfig("database")
//
//  implicit val httpHost = httpConfig.getString("interface")
//  implicit val httpPort = httpConfig.getInt("port")
//
//  implicit val jdbcUrl = dbConfig.getString("url")
//  implicit val dbUser = dbConfig.getString("user")
//  implicit val dbPassword = dbConfig.getString("password")
//
//  implicit val dbLimit = dbConfig.getInt("limit")
//  implicit val dbOffset = dbConfig.getInt("offset")
//}

trait DatabaseService extends ConfigHelper {
  import Thread._
  import Post._
  import DAO._

  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(jdbcUrl)
  hikariConfig.setUsername(dbUser)
  hikariConfig.setPassword(dbPassword)

  private val dataSource = new HikariDataSource(hikariConfig)
  val driver = slick.driver.PostgresDriver
  val db = Database.forDataSource(dataSource)

  db.createSession()

  /**
   *  Run schema setup if no setup is found
   */
  val ddl = threads.schema ++ posts.schema

  def createTablesIfNone(tables: Vector[MTable]) = {
    if (!tables.exists(_.name.name == threads.baseTableRow.tableName)) {
      db.run(ddl.create)
    } else {
      Future.successful("Initial database setup already in place.")
    }
  }

  val initialSetup = db.run(MTable.getTables).flatMap(createTablesIfNone)
  Await.result(initialSetup, Duration.Inf)
}
