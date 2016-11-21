package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
implicit val threadFormat = jsonFormat2(Thread.apply)

/** Implicit definition needed for UUID */
//  implicit val postFormat = jsonFormat6(Post.apply)
}

//  implicit object PostJsonFormat extends RootJsonFormat[Post] {
//    def write(p: Post) = {
//      JsObject(Map(
//        "pseudonym" -> JsString(p.pseudonym),
//        "email" -> JsString(p.email),
//        "content" -> JsString(p.content)))
//    }
//
//    def read(value: JsValue): Post = value.convertTo[Post]
//  }
//
//  implicit object ThreadJsonFormat extends RootJsonFormat[Thread[Post]] {
//    def write(t: Thread[Post]) = {
//      JsObject(Map(
//        "subject" -> JsString(t.subject),
//
//        /**
//         * - http://stackoverflow.com/questions/21756836/providing-a-jsonformat-for-a-sequence-of-objects
//         * - https://www.mendix.com/blog/nested-maps-json-scala/
//         * - http://tamagotchiconnectionv3.whoseopinion.com/tech-blog/serializing-json-generic-classes-spray-json
//         * "posts" -> JsObject(Map(t.posts.foreach(_._1.toJson) -> JsArray(write(t.posts))))))
//         */
//
//        "posts" -> JsObject(Map(t.posts.foreach(_._1.toJson) -> JsArray(write(t.posts))))))
//    }
//
//    def read(value: JsValue) = { value.convertTo[Thread[HashMap[Int, Post]]] }
//    def read(value: JsValue) = { value.convertTo[Thread[Post]] }

//    val json = ByteString(s"""
//            |{
//            |  "subject": "test",
//            |  "posts": {
//            |    "threadId": null,
//            |    "post": [
//            |      {
//            |        "pseudonym": "Michal",
//            |        "email": "michal.trzesimiech@gmail.com",
//            |        "content": "such content"
//            |      }
//            |    ]
//            |  }
//            |}""".stripMargin)

