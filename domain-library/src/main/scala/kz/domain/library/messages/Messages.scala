package kz.domain.library.messages

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

trait Sender

case class TelegramSender(telegramChatDetails: TelegramChatDetails)
    extends Sender

case class HttpSender(actorPath: String) extends Sender
