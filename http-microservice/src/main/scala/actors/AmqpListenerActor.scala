package actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import kz.domain.library.messages.UsersMessage
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object AmqpListenerActor {
  def props()(implicit system: ActorSystem): Props =
    Props(new AmqpListenerActor())
}

class AmqpListenerActor()(implicit system: ActorSystem)
    extends Actor
    with ActorLogging {
  implicit val formats: Formats = DefaultFormats
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      log.info(s"received message $msg")
      val actorMessage = parse(msg).extract[UsersMessage]
      val actor = system.actorSelection(actorMessage.args)
      actor ! actorMessage.message.head
  }
}
