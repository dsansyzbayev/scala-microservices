package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import kz.domain.library.messages.{BotResponse, HttpSender, SenderSerializers}
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object AmqpListenerActor {
  def props()(implicit system: ActorSystem): Props =
    Props(new AmqpListenerActor())
}

class AmqpListenerActor()(implicit system: ActorSystem)
    extends Actor
    with ActorLogging
    with SenderSerializers {
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      log.info(s"received message $msg")
      val actorMessage = parse(msg).extract[BotResponse]
      val sender = actorMessage.sender.asInstanceOf[HttpSender]
      val actor = system.actorSelection(sender.actorPath)
      actor ! actorMessage.response.head
  }
}
