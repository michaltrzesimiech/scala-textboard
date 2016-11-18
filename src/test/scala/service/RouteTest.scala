import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes, MediaTypes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.{ Directives }
import akka.http.scaladsl._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import akka.testkit._
import service.TextboardDb._
import service.TextboardJsonProtocol
import service.TextboardRoutes
import service.Universe._

class TestRoutes extends WordSpec with Matchers with ScalatestRouteTest {

  "The service" should {
    "list all threads with success" in {
      Get("/threads") ~> TextboardRoutes.route ~> check {
        status.isSuccess() shouldEqual true
      }
    }
  }

  //  "The service" should {
  //    "post new thread with success" in {
  //
  //val json = ByteString(s"""
  //|{
  //|  "subject": "test",
  //|  "posts": {
  //|    "threadId": null,
  //|    "post": [
  //|      {
  //|        "pseudonym": "Michal",
  //|        "email": "michal.trzesimiech@gmail.com",
  //|        "content": "such content"
  //|      }
  //|    ]
  //|  }
  //|}""".stripMargin)
  //
  //      val postRequest = HttpRequest(
  //        HttpMethods.POST,
  //        uri = "/customer",
  //        entity = HttpEntity(MediaTypes.`application/json`, json))
  //
  //      postRequest ~> TextboardRoutes.route ~> check {
  //        status.isSuccess() shouldEqual true
  //      }
  //    }
  //  }
}
