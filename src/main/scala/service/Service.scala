package main.scala.textboard

import akka.actor._
import akka.actor.{ Actor, Props, ActorRef }
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ HttpMethods, StatusCodes }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.{ Unmarshal, FromRequestUnmarshaller }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.util.Timeout
import java.util.UUID
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io._
import scala.io.StdIn
import scala.language.{ implicitConversions, postfixOps }
//import scala.util.{ Try, Success, Failure }
import spray.json._

object Service extends TextboardJsonProtocol with SprayJsonSupport {
  import akka.pattern.ask
  import DbActor._
  import WebServer._

  /**
   *  Summons DbActor
   */
  implicit val timeout: Timeout = Timeout(5 seconds)
  val master: ActorRef = system.actorOf(Props[DbActor], name = "master")

  /**
   * Returns the routes defined for endpoints:
   * 1. PUT			/thread/:thread_id/posts/:post_id?secret_id=x
   * x2. DELETE	/thread/:thread_id/posts/:post_id?secret_id=x
   * x3. GET			/thread/:thread_id/posts
   * v4. POST		/thread/:thread_id/posts
   * x5. GET			/threads?limit=x&offset=x
   * v6. POST		/thread
   *
   * @param system The implicit system to use for building routes
   * @param ec The implicit execution context to use for routes
   * @param mater The implicit materializer to use for routes
   */
  def route(implicit system: ActorSystem,
            ec: ExecutionContext,
            mater: Materializer): Route = {
    path("thread" / IntNumber / "posts" / IntNumber) { (threadId, postId) =>
      parameter('secret_id.as[String]) { secret_id =>
        put /** edit upon existing post in thread - 1 */ {
          entity(as[Post]) { post =>
            (master ? EditPost(postId, secret_id, post.content))
            complete(Future.successful(StatusCodes.OK))
          }
        } ~
          /**
           *  [info] [DEBUG] [11/27/2016 20:50:02.626] [default-akka.actor.default-dispatcher-27] [akka://default/user/StreamSuperviso
           * r-0/flow-5-0-unknown-operation] Aborting tcp connection because of upstream failure: No elements passed in the last 1 mi
           * nute.
           * [info] akka.stream.impl.Timers$IdleTimeoutBidi$$anon$7.onTimer(Timers.scala:160)
           * [info] akka.stream.stage.TimerGraphStageLogic.akka$stream$stage$TimerGraphStageLogic$$onInternalTimer(GraphStage.scala:1
           * 152)
           * [info] akka.stream.stage.TimerGraphStageLogic$$anonfun$akka$stream$stage$TimerGraphStageLogic$$getTimerAsyncCallback$1.a
           * pply(GraphStage.scala:1141)
           * [info] akka.stream.stage.TimerGraphStageLogic$$anonfun$akka$stream$stage$TimerGraphStageLogic$$getTimerAsyncCallback$1.a
           * pply(GraphStage.scala:1141)
           * [info] akka.stream.impl.fusing.GraphInterpreter.runAsyncInput(GraphInterpreter.scala:691)
           * [info] akka.stream.impl.fusing.GraphInterpreterShell.receive(ActorGraphInterpreter.scala:419)
           * [info] akka.stream.impl.fusing.ActorGraphInterpreter.akka$stream$impl$fusing$ActorGraphInterpreter$$processEvent(ActorGr
           * aphInterpreter.scala:603)
           * [info] akka.stream.impl.fusing.ActorGraphInterpreter$$anonfun$receive$1.applyOrElse(ActorGraphInterpreter.scala:618)
           * [info] akka.actor.Actor$class.aroundReceive(Actor.scala:484)
           * [info] akka.stream.impl.fusing.ActorGraphInterpreter.aroundReceive(ActorGraphInterpreter.scala:529)
           * [info] akka.actor.ActorCell.receiveMessage(ActorCell.scala:526)
           * [info] akka.actor.ActorCell.invoke(ActorCell.scala:495)
           * [info] akka.dispatch.Mailbox.processMailbox(Mailbox.scala:257)
           * [info] akka.dispatch.Mailbox.run(Mailbox.scala:224)
           * [info] akka.dispatch.Mailbox.exec(Mailbox.scala:234)
           * [info] scala.concurrent.forkjoin.ForkJoinTask.doExec(ForkJoinTask.java:260)
           * [info] scala.concurrent.forkjoin.ForkJoinPool$WorkQueue.runTask(ForkJoinPool.java:1339)
           * [info] scala.concurrent.forkjoin.ForkJoinPool.runWorker(ForkJoinPool.java:1979)
           * [info] scala.concurrent.forkjoin.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:107)
           * [info] [DEBUG] [11/27/2016 20:50:15.583] [default-akka.actor.default-dispatcher-26] [akka://default/system/IO-TCP/select
           * ors/$a/0] New connection accepted
           * [info] [INFO] [11/27/2016 20:50:15.607] [default-akka.actor.default-dispatcher-29] [WebServer$(akka://default)] List(sel
           * ect "SECRET" from "POSTS" where "ID" = 9)
           */
          delete /** post in thread - 2 */ {
            (master ? DeletePost(postId, secret_id))
            complete(Future.successful(StatusCodes.OK))
          }
      }
    } ~
      path("thread" / IntNumber / "posts") { threadId =>
        /** TODO: make this work */
        get /** all posts in specific thread - 3 */ {
          val formattedId = threadId.toLong
          val futOpenThread = (master ? OpenThread(formattedId)).mapTo[ToResponseMarshallable]
          //          val futOpenThread = (master ? OpenThread(formattedId)).mapTo[List[Post] /*ToResponseMarshallable*/ ]
          complete(futOpenThread)
        } ~
          /**
           * [info] [ERROR] [11/27/2016 20:45:21.825] [default-akka.actor.default-dispatcher-14] [akka.actor.ActorSystemImpl(default)
           * ] Error during processing of request HttpRequest(HttpMethod(GET),http://127.0.0.1:9000/thread/1/posts,List(Host: 127.0.0
           * .1:9000, Connection: keep-alive, Cache-Control: no-cache, User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebK
           * it/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36, Postman-Token: ff378414-da58-fb33-7be5-8f9e269a0d56, Ac
           * cept: , Accept-Encoding: gzip, deflate, sdch, br, Accept-Language: en-US, en;q=0.8, pl;q=0.6, Timeout-Access: <functi
           * on1>),HttpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))
           * [info] akka.pattern.AskTimeoutException: Ask timed out on [Actor[akka://default/user/master#-2140791726]] after [5000 ms
           * ]. Sender[null] sent message of type "main.scala.textboard.DbActor$OpenThread".
           * [info]  at akka.pattern.PromiseActorRef$$anonfun$1.apply$mcV$sp(AskSupport.scala:604)
           * [info]  at akka.actor.Scheduler$$anon$4.run(Scheduler.scala:126)
           * [info]  at scala.concurrent.Future$InternalCallbackExecutor$.unbatchedExecute(Future.scala:601)
           * [info]  at scala.concurrent.BatchingExecutor$class.execute(BatchingExecutor.scala:109)
           * [info]  at scala.concurrent.Future$InternalCallbackExecutor$.execute(Future.scala:599)
           * [info]  at akka.actor.LightArrayRevolverScheduler$TaskHolder.executeTask(LightArrayRevolverScheduler.scala:329)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.executeBucket$1(LightArrayRevolverScheduler.scala:280)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.nextTick(LightArrayRevolverScheduler.scala:284)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.run(LightArrayRevolverScheduler.scala:236)
           * [info]  at java.lang.Thread.run(Thread.java:745)
           * [info]
           */
          post /** reply to specific thread - 4 */ {
            entity(as[Post]) { post =>
              (master ? CreatePost(Some(threadId.toLong),
                post.pseudonym,
                post.email,
                post.content)).mapTo[Post]
              complete(StatusCodes.Created)
            }
          }
      } ~
      path("threads") {
        /** TODO Make this print out an unordered list of threads */
        get /** all threads - 5 */ {
          /**
           * [info] [INFO] [11/27/2016 20:39:29.335] [default-akka.actor.default-dispatcher-3] [WebServer$(akka://default)] List(sele
           * ct "THREAD_ID", "SUBJECT" from "THREADS")
           * [info] [ERROR] [11/27/2016 20:39:34.340] [default-akka.actor.default-dispatcher-3] [akka.actor.ActorSystemImpl(default)]
           * Error during processing of request HttpRequest(HttpMethod(GET),http://127.0.0.1:9000/threads,List(Host: 127.0.0.1:9000,
           * Connection: keep-alive, Cache-Control: no-cache, User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.3
           * 6 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36, Postman-Token: cde65a3b-6773-91d0-09f1-4b7f92ef65dd, Accept:
           * , Accept-Encoding: gzip, deflate, sdch, br, Accept-Language: en-US, en;q=0.8, pl;q=0.6, Timeout-Access: <function1>),Ht
           * tpEntity.Strict(none/none,ByteString()),HttpProtocol(HTTP/1.1))
           * [info] akka.pattern.AskTimeoutException: Ask timed out on [Actor[akka://default/user/master#-2140791726]] after [5000 ms
           * ]. Sender[null] sent message of type "main.scala.textboard.DbActor$SimplyListAllThreads$".
           * [info]  at akka.pattern.PromiseActorRef$$anonfun$1.apply$mcV$sp(AskSupport.scala:604)
           * [info]  at akka.actor.Scheduler$$anon$4.run(Scheduler.scala:126)
           * [info]  at scala.concurrent.Future$InternalCallbackExecutor$.unbatchedExecute(Future.scala:601)
           * [info]  at scala.concurrent.BatchingExecutor$class.execute(BatchingExecutor.scala:109)
           * [info]  at scala.concurrent.Future$InternalCallbackExecutor$.execute(Future.scala:599)
           * [info]  at akka.actor.LightArrayRevolverScheduler$TaskHolder.executeTask(LightArrayRevolverScheduler.scala:329)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.executeBucket$1(LightArrayRevolverScheduler.scala:280)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.nextTick(LightArrayRevolverScheduler.scala:284)
           * [info]  at akka.actor.LightArrayRevolverScheduler$$anon$4.run(LightArrayRevolverScheduler.scala:236)
           * [info]  at java.lang.Thread.run(Thread.java:745)
           */
          val query = (master ? SimplyListAllThreads).mapTo[ToResponseMarshallable]
          //            val futListAllThreads = (master ? ListAllThreads(10, 10)).mapTo[ToResponseMarshallable]
          complete(query)
        }
      } ~
      post /** new thread - 6 */ {
        entity(as[NewThread]) { thread =>
          (master ? CreateThread(thread.threadId, thread.subject)).mapTo[Thread]

          /**
           * ERROR:  null value in column "THREAD_ID" violates not-null constraint
           * DETAIL:  Failing row contains (7, null, 47e1d8a1-b6ff-47a6-817c-cf0fa688b3b3, commenter, hq@commenter.com, 20:06).
           * STATEMENT:  insert into "POSTS" ("THREAD_ID","SECRET","PSEUDONYM","EMAIL","CONTENT")  values ($1,$2,$3,$4,$5)
           */
          (master ? CreatePost(thread.threadId, thread.pseudonym, thread.email, thread.content)).mapTo[Post]
          complete(Future.successful(StatusCodes.Created))
        }
      }
  }
}

/** TODO 1:
 *  X. Make all routes work
 *  X. Secret verification to check
 *  X. Add basic checks for fail
 *  X. Add indexes
 *  X. Add validation https://github.com/wix/accord
 *  X. Add pagination
 */