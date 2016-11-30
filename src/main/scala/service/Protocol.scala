package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers //!
import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller //!
import com.wix.accord.dsl._
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
  //      case n: Int => JsNumber(n)
  //      case s: String => JsString(s)
  //      case b: Boolean if b == true => JsTrue
  //      case b: Boolean if b == false => JsFalse
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
  implicit val postFormat = jsonFormat6(Post.apply)
  implicit val newThread = jsonFormat6(NewThread.apply)
  implicit val newContent = jsonFormat1(NewContent.apply)

  /** TODO: Doesn't catch violation */
  implicit val threadValidation = validator[NewThread] { thread =>
    thread.subject as "subject" is notEmpty
    thread.subject.length() as "subject" should be > 2
  }
  
  /** TODO: Doesn't catch violation */
  implicit val postValidation = validator[Post] { post =>
    post.content as "content" is notEmpty
    post.email as "email" is notEmpty
    post.email.length() as "email:length" should be > 5
    post.pseudonym as "pseudonym" is notEmpty
  }

}
