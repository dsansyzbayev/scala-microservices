package actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.domain.library.messages.{SenderSerializers, UserRequest}
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel,
            exchangeChatGatewayIn: String,
            routingKeyIn: String): Props =
    Props(new AmqpPublisherActor(channel, exchangeChatGatewayIn, routingKeyIn))

}

class AmqpPublisherActor(channel: Channel,
                         exchangeChatGatewayIn: String,
                         routingKeyIn: String)
    extends Actor
    with ActorLogging
    with SenderSerializers {

  override def receive: Receive = {
    case msg: UserRequest =>
      log.info(s"sending message: $msg to AMQP")
      val jsonMessage: String = write(msg)

      Try(
        channel.basicPublish(
          exchangeChatGatewayIn,
          routingKeyIn,
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes()
        )
      ) match {
        case Success(_) => log.info(s"successfully send message $jsonMessage")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }
  }
}
