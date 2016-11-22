package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.ByteString
import java.util.UUID
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
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



