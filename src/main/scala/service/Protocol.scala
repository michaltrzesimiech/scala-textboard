package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
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
  //      case u: UUID                  => JsString(u.toString)
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

  //  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
  //    def write(x: UUID) = JsString(x.toString) //Never execute this line
  //    def read(value: JsValue) = value match {
  //      case JsString(x) => UUID.fromString(x)
  //      case x           => deserializationError("Expected UUID as JsString, but got " + x)
  //    }
  //  }

  //  http://labs.unacast.com/2016/03/03/building-microservices-with-akka-http/
  //  implicit val threadValidation = validator[Thread] { thread =>
  //    thread.x is notEmpty
  //    thread.y is notEmpty
  //    thread.z.length() as "password:length" should be > 5
  //  }

  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat6(Post.apply)

  implicit val newThreadFormat = jsonFormat5(NewThread.apply)

}