package http.routes

import actors.PerRequest.PerRequestActor
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.util.Timeout
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import http.routes.AllRoutes.{BasicRequest, Request}
import org.json4s.{DefaultFormats, Serialization}
import org.json4s.jackson.Serialization
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.DurationInt

object AllRoutes {
  trait Request
  case class BasicRequest(message: String) extends Request
}

class AllRoutes(publisher: ActorRef, routingKey: String)(
    implicit ex: ExecutionContext,
    system: ActorSystem)
    extends Json4sSupport {
  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val timeout: Timeout = 5.seconds

  val handlers: Route = pathPrefix("api") {
    pathPrefix("bot") {
      path("talk") {
        post {
          entity(as[BasicRequest]) { body => ctx =>
            completeRequest(body.message, body, ctx)
          }
        }
      }
    }
  }

  def completeRequest(bodyMessage: String,
                      body: Request,
                      ctx: RequestContext): Future[RouteResult] = {
    val promise = Promise[RouteResult]
    system.actorOf(
      Props(
        new PerRequestActor(routingKey,
                            bodyMessage,
                            body,
                            promise,
                            ctx,
                            publisher)))
    promise.future
  }
}
