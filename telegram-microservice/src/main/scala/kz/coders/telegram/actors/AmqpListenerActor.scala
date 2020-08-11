package kz.coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.bot4s.telegram.models.{Chat, ChatType, Message, User}
import kz.coders.telegram.TelegramService
import kz.domain.library.messages.{BotResponse, Sender, SenderSerializers, TelegramSender}
import org.json4s.jackson.JsonMethods.parse
import cats.instances.future._
import cats.syntax.functor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object AmqpListenerActor {
  def props(telegramService: TelegramService): Props =
    Props(new AmqpListenerActor(telegramService))
}

class AmqpListenerActor(telegramService: TelegramService)
    extends Actor
    with ActorLogging with SenderSerializers{
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      val usersMessage = parse(msg).extract[BotResponse]
      val senderDetails = usersMessage.sender.asInstanceOf[TelegramSender].telegramChatDetails

      telegramService.replyToUser(usersMessage.response.getOrElse(""), senderDetails)

      log.info(s"received message $msg")
  }

}
