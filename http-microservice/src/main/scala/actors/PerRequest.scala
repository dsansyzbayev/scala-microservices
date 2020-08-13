package actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.server.{RequestContext, RouteResult}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import http.routes.AllRoutes.Request
import kz.domain.library.messages.{HttpSender, UserRequest}
import org.json4s.{DefaultFormats, Formats, Serialization}
import org.json4s.native.Serialization

import scala.concurrent.{ExecutionContext, Promise}

object PerRequest {
  class PerRequestActor(val routingKey: String,
                        val body: String,
                        val request: Request,
                        val promise: Promise[RouteResult],
                        val requestContext: RequestContext,
                        val publisherActor: ActorRef)
      extends PerRequest {
    val httpSender: HttpSender = HttpSender(self.path.toStringWithoutAddress)
    val httpMessage: UserRequest =
      UserRequest(Option(body), httpSender, routingKey)
    publisherActor ! httpMessage
  }

}

trait PerRequest extends Actor with ActorLogging with Json4sSupport {

  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val ex: ExecutionContext = context.dispatcher

  val request: Request
  val promise: Promise[RouteResult]
  val requestContext: RequestContext

  override def receive: Receive = {
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
