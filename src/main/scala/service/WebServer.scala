package main.scala.textboard

import akka.actor.ActorSystem
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.stream.{ ActorMaterializer, Materializer }
import com.typesafe.config.{ Config, ConfigFactory }
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future, Await }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions }

object WebServer extends App with DatabaseService {

  import DAO._
  import Service._

  /**
   * Invokes ActorSystem, materialises Actor and execution context
   */
  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val mater: ActorMaterializer = ActorMaterializer()

  /**
   * Creates tables, then fills them with dummy data
   */
  //  Await.result(db.run(initSetup), Duration.Inf)

  val config: Config = ConfigFactory.load()
  val log: LoggingAdapter = Logging(system, getClass)

  /**
   *  Binds routes to server, gracefully terminates DB and server when done
   */
  val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
  println(s"Server running. Press RETURN to stop."); StdIn.readLine()
  db.close
  binding
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}