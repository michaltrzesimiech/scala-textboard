//import org.scalatest.{ Matchers, WordSpec }
//import akka.http.scaladsl.model.{ HttpRequest, HttpEntity, HttpMethods, StatusCodes, MediaTypes }
//import akka.http.scaladsl.server._
//import akka.http.scaladsl.server.{ Directives }
//import akka.http.scaladsl._
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import akka.util.ByteString
//import akka.testkit._
//import scala.concurrent.{ Future, Await }
//import scala.concurrent.duration._
//import slick.lifted.{ AbstractTable, Case }
//import slick.jdbc.JdbcBackend.Database
//
//import main.scala.textboard._
//
//class ServiceSpec extends WordSpec with Matchers with ScalatestRouteTest /*with DatabaseService*/ {
//
//  import main.scala.textboard
//  import tempJson._
//
//  import Service._
//  val route = Service.route
//
//  "The service" should {
//    "post new thread" in {
//      postThreadRequest ~> route ~> check {
//        status.isSuccess() shouldEqual true
//      }
//    }
//  }
//
//  "The service" should {
//    "list all threads" in {
//      Get("/threads?limit=10&offset=x") ~> route ~> check {
//        status.isSuccess() shouldEqual true
//      }
//    }
//  }
//
//  "The service" should {
//
//    "post reply to a thread" in {
//      postPostRequest ~> route ~> check {
//        status.isSuccess() shouldEqual true
//      }
//    }
//  }
//
//  "The service" should {
//
//    "list all replies in a thread" in {
//      Get("/thread/1/posts") ~> route ~> check {
//        status.isSuccess() shouldEqual true
//      }
//    }
//  }
//  //    "delete a post given secret key" in {
//  //      Get("/thread/1/posts/1?secret_id=?") ~> route ~> check {
//  //        status.isSuccess() shouldEqual true
//  //      }
//  //    }
//  //
//  //    "edit a post given secret key" in {
//  //      Get("/thread/1/posts/?") ~> route ~> check {
//  //        status.isSuccess() shouldEqual true
//  //      }
//  //    }
//}
//
//object tempJson {
//
//  val postThreadRequest = HttpRequest(
//    HttpMethods.POST,
//    uri = "/threads",
//    entity = HttpEntity(MediaTypes.`application/json`, tempJson.jsonThread))
//
//  val postPostRequest = HttpRequest(
//    HttpMethods.POST,
//    uri = "/thread/1/posts",
//    entity = HttpEntity(MediaTypes.`application/json`, tempJson.jsonThread))
//
//  val jsonThread = ByteString(s"""
//|{
//|  "threadId": null,
//|  "subject": "test subject"
//|}""".stripMargin)
//
//  val jsonPost = ByteString(s"""
//|{
//|    "postId": null,
//|    "threadId": 1,
//|    "secretId": 10010010,
//|    "pseudonym": "commenter",
//|    "email": "hq@commenter.com",
//|    "content": "tentatively triggered"
//|}""".stripMargin)
//}