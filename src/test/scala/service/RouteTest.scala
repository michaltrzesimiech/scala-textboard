import org.scalatest.{ Matchers, WordSpec }
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server._
import Directives._

import service.TextboardDb._
import service.TextboardJsonProtocol
import service.Universe._
import service.TextboardRoutes

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
//      Post("/threads") ~> TextboardRoutes.route ~> check {
//        status.isSuccess() shouldEqual true
//      }
//    }
//  }

}