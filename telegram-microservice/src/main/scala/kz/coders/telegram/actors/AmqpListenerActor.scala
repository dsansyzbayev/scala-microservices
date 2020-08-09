package kz.coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.bot4s.telegram.models.{Chat, ChatType, Message, User}
import kz.coders.telegram.{TelegramChatDetails, TelegramService}
import kz.domain.library.messages.UsersMessage
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats}
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
    with ActorLogging {
  implicit val formats: Formats = DefaultFormats
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      val usersMessage = parse(msg).extract[UsersMessage]
      val senderDetails = parse(usersMessage.args).extract[TelegramChatDetails]

      telegramService
        .reply(usersMessage.message.head) {
          Message(
            messageId = 1,
            from = Some(
              User(
                senderDetails.userId,
                isBot = false,
                senderDetails.firstname,
                senderDetails.lastname,
                senderDetails.username,
                Some("ru")
              )
            ),
            date = 1,
            chat = Chat(
              id = senderDetails.chatId,
              `type` = ChatType.Private,
              username = senderDetails.username,
              firstName = Some(senderDetails.firstname),
              lastName = senderDetails.lastname
            )
          )
        }
        .void

      log.info(s"received message $msg")
  }

}
