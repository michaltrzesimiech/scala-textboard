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
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import com.typesafe.config.ConfigFactory

trait ConfigHelper {
  private val config = ConfigFactory.load()

  private val httpConfig = config.getConfig("http")
  private val dbConfig = config.getConfig("database")

  implicit val httpHost = httpConfig.getString("interface")
  implicit val httpPort = httpConfig.getInt("port")

  implicit val jdbcUrl = dbConfig.getString("url")
  implicit val dbUser = dbConfig.getString("user")
  implicit val dbPassword = dbConfig.getString("password")
}

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

  val ddl = threads.schema ++ posts.schema
  implicit def secretId: String = UUID.randomUUID.toString()

  val initSetup = {
    DBIO.seq(
      /**
       *  Create tables, including primary and foreign keys
       */
      ddl.create,

      /**
       *  Insert dummy threads and posts
       */
      threads += Thread(None, "SUBJECT A"),
      threads += Thread(None, "SUBJECT B"),
      posts ++= Seq(
        Post(None, 1, secretId, "Agent A", "agent@one.com", "COMMENT"),
        Post(None, 1, secretId, "Agent B", "author@other.com", "COMMENT"),
        Post(None, 2, secretId, "Agent C", "author@troll.lol", "COMMENT")))
  }
  //  val futureInitialSetup: Future[Unit] = db.run(initSetup)
}