package kz.domain.library.messages

import org.json4s.{Formats, ShortTypeHints}
import org.json4s.jackson.Serialization

final case class TelegramChatDetails(
    username: Option[String],
    firstname: String,
    lastname: Option[String],
    chatId: Long,
    messageId: Int,
    userId: Int
)

case class UserRequest(message: Option[String], sender: Sender, replyTo: String)

case class BotResponse(response: Option[String],
                       sender: Sender,
                       replyTo: String)

trait Sender {
  implicit val formats: AnyRef with Formats =
    Serialization.formats(
      ShortTypeHints(List(classOf[TelegramSender], classOf[HttpSender])))
}

case class TelegramSender(telegramChatDetails: TelegramChatDetails)
    extends Sender

case class HttpSender(actorPath: String) extends Sender
