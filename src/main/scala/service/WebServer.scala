package main.scala.textboard

import akka.actor.ActorSystem
import akka.actor.{ Actor, Props }
import akka.event.{ LoggingAdapter, Logging }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import com.typesafe.config.{ Config, ConfigFactory }
import scala.collection.mutable.{ Seq, HashMap }
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
import slick._
import slick.util._
import slick.driver.PostgresDriver.api._
import slick.lifted.{ AbstractTable, Rep, ProvenShape }
import spray.json._
import spray.json.DefaultJsonProtocol

object WebServer extends App {
  Service.run
  /**
   * TODO: Enable here:
   *
   * /** Invoke ActorSystem, materializes Actor and execution context */
   * override implicit val system = ActorSystem()
   * override implicit val ec: ExecutionContext = system.dispatcher
   * override implicit val mater: ActorMaterializer = ActorMaterializer()
   *
   * override val log: LoggingAdapter = Logging(system, getClass)
   * override val config = ConfigFactory.load()
   *
   * /** Bind routes to server, gracefully terminate server when done */
   * val binding = Http().bindAndHandle(route, config.getString("http.interface"), config.getInt("http.port"))
   * println(s"Server running. Press ENTER to stop."); StdIn.readLine()
   * binding
   * .flatMap(_.unbind())
   * .onComplete(_ => system.terminate())
   */
}

