package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.ByteString
import java.util.UUID
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol

/**
 * Root json protocol class for others to extend from
 */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any) = x match {
      case n: Int                   => JsNumber(n)
      case s: String                => JsString(s)
      case u: UUID                  => JsString(u.toString)
      case b: Boolean if b == true  => JsTrue
      case b: Boolean if b == false => JsFalse
    }
    def read(value: JsValue) = value match {
      case JsNumber(n) => n.intValue()
      case JsString(s) => s
      case JsArray(a)  => List(a)
      case JsTrue      => true
      case JsFalse     => false
    }
  }

  implicit object UuidJsonFormat extends RootJsonFormat[UUID] {
    def write(x: UUID) = JsString(x.toString) //Never execute this line
    def read(value: JsValue) = value match {
      case JsString(x) => UUID.fromString(x)
      case x           => deserializationError("Expected UUID as JsString, but got " + x)
    }
  }

  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat6(Post.apply)
}

/** TODO: Delete this when no longer required */
object tempJson {
  val jsonThread = ByteString(s"""
|{
|  "threadId": null,
|  "subject": "test subject"
|}""".stripMargin)

  val jsonPost = ByteString(s"""
|{
|    "postId": null,
|    "threadId": 1,
|    "secretId": 10010010,
|    "pseudonym": "commenter",
|    "email": "hq@commenter.com",
|    "content": "tentatively triggered"
|}""".stripMargin)
}

