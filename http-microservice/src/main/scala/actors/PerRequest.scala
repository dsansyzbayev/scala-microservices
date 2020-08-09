package actors

import actors.PerRequest.PerRequestResponse
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import http.routes.AllRoutes.Request
import kz.domain.library.messages.UsersMessage
import org.json4s.{DefaultFormats, Formats, Serialization}
import org.json4s.native.Serialization

import scala.concurrent.{ExecutionContext, Promise}

object PerRequest {
  class PerRequestActor(val body: String,
                        val request: Request,
                        val childProps: Props,
                        val promise: Promise[RouteResult],
                        val requestContext: RequestContext,
                        val publisherActor: ActorRef)
      extends PerRequest {
    val httpMessage =
      UsersMessage("http", Option(body), "", self.path.toStringWithoutAddress)
    publisherActor ! httpMessage
  }

  trait PerRequestResponse
}

trait PerRequest extends Actor with ActorLogging with Json4sSupport {

  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val ex: ExecutionContext = context.dispatcher

  val request: Request
  val childProps: Props
  val promise: Promise[RouteResult]
  val requestContext: RequestContext

  context.actorOf(childProps) ! request

  log.warning(s"received request actor ${self.path.name}")

  override def receive: Receive = {
    case obj: PerRequestResponse =>
      populateResponse(obj)
    case obj: String =>
      populateResponse(obj)
  }

  def populateResponse(obj: ToResponseMarshallable): Unit = {
    requestContext
      .complete(obj)
      .onComplete(something => promise.complete(something))

    context.stop(self)
  }

  override def postStop(): Unit =
    log.warning("I stopped and removed myself from memory")
}
