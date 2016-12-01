package main.scala.textboard

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers._
import akka.http.scaladsl.marshalling.GenericMarshallers.futureMarshaller
import spray.json._
import spray.json.DefaultJsonProtocol

/**
 * Root json protocol class for others to extend from.
 */
trait TextboardJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val threadFormat = jsonFormat2(Thread.apply)
  implicit val postFormat = jsonFormat6(Post.apply)
  implicit val newThread = jsonFormat6(NewThread.apply)
  implicit val newContent = jsonFormat1(NewContent.apply)
}