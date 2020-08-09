package kz.coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.coders.telegram.TelegramChatDetails
import kz.domain.library.messages.UsersMessage
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class SendMessage(message: String,
                         telegramChatDetails: TelegramChatDetails)
}

class AmqpPublisherActor(channel: Channel) extends Actor with ActorLogging {
  implicit val formats: DefaultFormats = DefaultFormats

  override def receive: Receive = {
    case msg: UsersMessage =>
      log.info(s"sending message to AMQP")
      val jsonMessage: String = write(msg)

      Try(
        channel.basicPublish(
          "X:chat-gateway",
          "user.chat.message",
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes()
        )
      ) match {
        case Success(_) => log.info(s"successfully send message $msg")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }
  }
}
