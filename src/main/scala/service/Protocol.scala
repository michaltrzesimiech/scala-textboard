package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers._
import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import spray.json._
import spray.json.DefaultJsonProtocol
import textboard.domain._

import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import java.sql.Timestamp
import slick.driver.PostgresDriver.api._
import slick.ast.TypedType

/**
 * Root json protocol class for others to extend from.
 */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object DateTimeFromat extends RootJsonFormat[DateTime] {
    def read(value: JsValue) = value match {
      case dt: JsValue => value.convertTo[DateTime]
      case _ => deserializationError("DateTime expected")
    }
    def write(c: DateTime) = JsString(c.toString)
  }

  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat7(Post.apply)
  implicit val newThread = jsonFormat6(NewThread.apply)
  implicit val newContent = jsonFormat1(NewContent.apply)

}