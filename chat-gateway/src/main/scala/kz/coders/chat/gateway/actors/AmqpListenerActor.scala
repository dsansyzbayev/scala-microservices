package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import kz.domain.library.messages.{Sender, UserRequest}
import org.json4s.jackson.JsonMethods.parse
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object AmqpListenerActor {
  def props(dialogflowActor: ActorRef)(implicit system: ActorSystem): Props =
    Props(new AmqpListenerActor(dialogflowActor))
}

class AmqpListenerActor(dialogflowActor: ActorRef)(implicit system: ActorSystem)
    extends Actor
    with ActorLogging
    with Sender {
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      log.info(s"received message $msg")
      val userMessage = parse(msg).extract[UserRequest]
      dialogflowActor ! userMessage

  }

}
