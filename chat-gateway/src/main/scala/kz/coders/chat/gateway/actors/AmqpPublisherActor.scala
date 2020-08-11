package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.domain.library.messages.{BotResponse, Sender, SenderSerializers}
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, exchangeChatGatewayOut: String)(
      implicit system: ActorSystem): Props =
    Props(new AmqpPublisherActor(channel, exchangeChatGatewayOut))
}

class AmqpPublisherActor(channel: Channel, exchangeChatGatewayOut: String)(
    implicit system: ActorSystem)
    extends Actor
    with ActorLogging
    with SenderSerializers {

  override def receive: Receive = {
    case msg: BotResponse =>
      log.info(s"sending message: $msg to AMQP")
      val jsonMessage: String = write(msg)

      publishToX(exchangeChatGatewayOut, msg.replyTo, jsonMessage)
  }

  def publishToX(exchange: String,
                 routingKey: String,
                 jsonMessage: String): Unit =
    Try(
      channel.basicPublish(
        exchange,
        routingKey,
        MessageProperties.TEXT_PLAIN,
        jsonMessage.getBytes()
      )
    ) match {
      case Success(_) => log.info(s"successfully send message $jsonMessage")
      case Failure(exception) =>
        log.warning(s"couldn't message ${exception.getMessage}")
    }
}
