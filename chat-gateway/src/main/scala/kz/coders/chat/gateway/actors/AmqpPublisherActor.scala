package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.domain.library.messages.UsersMessage
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel)(implicit system: ActorSystem): Props =
    Props(new AmqpPublisherActor(channel))

  final case class TelegramChatDetails(
      username: Option[String],
      firstname: String,
      lastname: Option[String],
      chatId: Long,
      messageId: Int,
      userId: Int
  )
}

class AmqpPublisherActor(channel: Channel)(implicit system: ActorSystem)
    extends Actor
    with ActorLogging {
  implicit val formats: DefaultFormats.type = DefaultFormats

  override def receive: Receive = {
    case msg: UsersMessage =>
      log.info(s"sending message to AMQP")
      val jsonMessage: String = write(msg)

      msg.senderPlatform match {
        case "http" =>
          publishToX("X:http-microservice", "user.http.message", jsonMessage)
        case "telegram" =>
          publishToX("X:telegram-microservice",
                     "user.chat.message",
                     jsonMessage)
      }
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
