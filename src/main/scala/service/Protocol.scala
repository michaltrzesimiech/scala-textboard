package service

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{ ToResponseMarshallable, ToResponseMarshaller }
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object ThreadJsonFromat extends RootJsonFormat[Thread] {

    /** JSON => Thread */
    def read(value: JsValue) = value match {
      case obj: JsObject if (obj.fields.size == 2) => value.convertTo[Thread]
      case _                                       => deserializationError("Thread expected")
    }

    /** Thread => JSON (JsArray(JsNumber(t.threadId), JsString(t.subject), JsObject(t.posts))) */
    def write(t: Thread) = t.toJson
  }

  implicit val postFormat = jsonFormat3(Post.apply)
}