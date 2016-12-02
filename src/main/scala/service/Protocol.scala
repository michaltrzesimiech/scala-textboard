package textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers._
import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller
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

  //  implicit object AnyJsonFormat extends JsonFormat[Any] {
  //    def write(x: Any) = x match {
  //      case n: Int => JsNumber(n)
  //      case s: String => JsString(s)
  //      case b: Boolean if b == true => JsTrue
  //      case b: Boolean if b == false => JsFalse
  //      case dt: DateTime => JsString(dt)
  //      case ts: Timestamp => JsString(dt)
  //    }
  //    def read(value: JsValue) = value match {
  //      case JsNumber(n) => n.intValue()
  //      case JsString(s) => s
  //      case JsArray(a) => List(a)
  //      case JsNull => null
  //      case JsTrue => true
  //      case JsFalse => false
  //    }
  //  }

  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat7(Post.apply)
  implicit val newThread = jsonFormat6(NewThread.apply)
  implicit val newContent = jsonFormat1(NewContent.apply)
}