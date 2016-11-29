package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers //!
import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller //!
import java.util.UUID
import scala.concurrent.Future
import spray.json._
import spray.json.DefaultJsonProtocol

/**
 * Root json protocol class for others to extend from
 */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  //  implicit object AnyJsonFormat extends JsonFormat[Any] {
  //    def write(x: Any) = x match {
  //      case n: Int                   => JsNumber(n)
  //      case s: String                => JsString(s)
  //      case b: Boolean if b == true  => JsTrue
  //      case b: Boolean if b == false => JsFalse
  //    }
  //    def read(value: JsValue) = value match {
  //      case JsNumber(n) => n.intValue()
  //      case JsString(s) => s
  //      case JsArray(a)  => List(a)
  //      case JsNull      => null
  //      case JsTrue      => true
  //      case JsFalse     => false
  //    }
  //  }

  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat6(Post.apply)
  implicit val newThread = jsonFormat6(NewThread.apply)
}