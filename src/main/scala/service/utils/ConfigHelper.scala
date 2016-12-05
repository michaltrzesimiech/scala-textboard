package textboard.utils

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape, Case }
import slick.util._

trait ConfigHelper {
  private val config = ConfigFactory.load()

  private val httpConfig = config.getConfig("http")
  private val dbConfig = config.getConfig("database")

  implicit val httpHost = httpConfig.getString("interface")
  implicit val httpPort = httpConfig.getInt("port")

  implicit val jdbcUrl = dbConfig.getString("url")
  implicit val dbUser = dbConfig.getString("user")
  implicit val dbPassword = dbConfig.getString("password")

  implicit val dbLimit = dbConfig.getInt("limit")
  implicit val dbOffset = dbConfig.getInt("offset")
}