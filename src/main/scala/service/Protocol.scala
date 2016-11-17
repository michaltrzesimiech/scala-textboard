package service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit object ThreadJsonFromat extends RootJsonFormat[Thread] {

    def read(value: JsValue) = {
      value.convertTo[Thread]
      /**
       * TODO: Add pattern matching once the domain model is final
       * value match {
       * case obj: JsObject if (obj.fields.size == 3) => value.convertTo[Thread]
       * case _                                       => deserializationError("Thread expected") }
       */
    }

    /** Thread => JSON (JsArray(JsNumber(t.threadId), JsObject(t.posts))) */
    def write(t: Thread) = t.toJson
  }

  implicit val postFormat = jsonFormat3(Post.apply)
}