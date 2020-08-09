package actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import org.json4s.DefaultFormats

object ResponseActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ResponseActor())
}
class ResponseActor()(implicit system: ActorSystem, materializer: Materializer)
    extends Actor
    with ActorLogging {
  implicit val ex = context.dispatcher
  implicit val formats = DefaultFormats

  override def receive: Receive = {
    case msg: String =>
      log.info(s"received message: $msg")
      val senderRef = sender()
      senderRef ! msg
  }
}
