package service

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol
import akka.util.ByteString

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  // implicit val threadFormat = jsonFormat6(Thread[Post])
  implicit val postFormat = jsonFormat3(Post)

  implicit object PostJsonFormat extends RootJsonFormat[Post] {

    def write(p: Post) = {
      JsObject(Map(
        "pseudonym" -> JsString(p.pseudonym),
        "email" -> JsString(p.email),
        "content" -> JsString(p.content)))
    }

    def read(value: JsValue): Post = value.convertTo[Post]
  }

  implicit object ThreadJsonFormat extends RootJsonFormat[Thread[Post]] {

    /** Thread => JSON (JsArray(JsNumber(t.threadId), JsObject(t.posts))) */
    def write(t: Thread[Post]) = {
      JsObject(Map(
        "subject" -> JsString(t.subject),

        /** Cannot be used with HashMap */
        "posts" -> JsArray(t.posts)))
    }

    def read(value: JsValue) = { value.convertTo[Thread[Post]] }

    // val json = ByteString(s"""
    // |{
    // | "subject": "test",
    // | "posts": {
    // | "threadId": null,
    // | "post": [
    // | {
    // | "pseudonym": "Michal",
    // | "email": "michal.trzesimiech@gmail.com",
    // | "content": "such content"
    // | }
    // | ]
    // | }
    // |}""".stripMargin)

  }
}

