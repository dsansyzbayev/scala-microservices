package kz.domain.library.messages

case class UsersMessage(senderPlatform: String,
                        message: Option[String],
                        command: String,
                        args: String)

case class GatewayResponse(response: String)
