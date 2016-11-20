package textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import scala.language.{ implicitConversions }
import spray.json._
import spray.json.DefaultJsonProtocol
import akka.util.ByteString
import scala.collection.mutable.{ Seq, HashMap }

trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit def threadFormat[Post: JsonFormat] = jsonFormat2(Thread.apply[Post])
  implicit val postFormat = jsonFormat3(Post.apply)

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

        /**
         * Trying spree
         * - http://stackoverflow.com/questions/21756836/providing-a-jsonformat-for-a-sequence-of-objects
         * - https://www.mendix.com/blog/nested-maps-json-scala/
         * - resign from maps or provide extra marshaller for Map[Int, Post]
         * - http://stackoverflow.com/questions/25916419/serialize-mapstring-any-with-spray-json
         * - https://groups.google.com/forum/#!topic/spray-user/FJ4Mi2pzTtM
         * - https://github.com/debasishg/sjson/wiki/Examples-of-Type-Class-based-JSON-serialization
         * - http://stackoverflow.com/questions/21756836/providing-a-jsonformat-for-a-sequence-of-objects
         * - http://tamagotchiconnectionv3.whoseopinion.com/tech-blog/serializing-json-generic-classes-spray-json
         *
         * - know what this is doing
         * - know what you want to achieve, write it out
         * - look for examples in other projects
         * - try other json libraries
         *
         * "posts" -> JsArray(t.posts)))
         * "posts" -> write(t.posts.map(_.toJson).toList)))
         * "posts" -> JsArray(t.posts.keys.map(_.toJson), write(t.posts.map(_.toJson).toList))))
         * "posts" -> JsArray(Map(t.posts.foreach(_._1.toJson) -> write(t.posts.map(_.toJson).toList)))
         * "posts" -> JsObject(Map(t.posts.foreach(_._1.toJson) -> JsArray(write(t.posts))))))
         * "posts" -> JsObject(Map(t.posts.foreach(_._1.toJson) -> JsArray(write(t.posts))))))
         */

        "posts" -> JsObject(Map(t.posts.foreach(_._1.toJson) -> JsArray(write(t.posts))))))
    }

    //    def read(value: JsValue) = { value.convertTo[Thread[HashMap[Int, Post]]] }
    def read(value: JsValue) = { value.convertTo[Thread[Post]] }

    //    val json = ByteString(s"""
    //    		|{
    //    		|  "subject": "test",
    //    		|  "posts": {
    //    		|    "threadId": null,
    //    		|    "post": [
    //    		|      {
    //    		|        "pseudonym": "Michal",
    //    		|        "email": "michal.trzesimiech@gmail.com",
    //    		|        "content": "such content"
    //    		|      }
    //    		|    ]
    //    		|  }
    //    		|}""".stripMargin)
  }
}