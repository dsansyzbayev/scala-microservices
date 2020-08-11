package kz.coders.telegram

import akka.actor.ActorRef
import akka.util.Timeout
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.clients.FutureSttpClient
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import cats.instances.future._
import cats.syntax.functor._
import kz.domain.library.messages.{
  TelegramChatDetails,
  TelegramSender,
  UserRequest
}
import org.json4s.DefaultFormats
import scala.concurrent.Future
import scala.concurrent.duration._

class TelegramService(token: String,
                      publisherActor: ActorRef,
                      routingKeyT: String)
    extends TelegramBot
    with Polling
    with Commands[Future] {

  implicit val timeout: Timeout = 5.seconds
  implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()
  implicit val formats: DefaultFormats = DefaultFormats
  override val client: RequestHandler[Future] = new FutureSttpClient(token)

  onCommand("/start") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    reply("Привет").void
  }

  onMessage { implicit msg =>
    val telegramChatDetails = {
      TelegramChatDetails(
        msg.from.head.username,
        msg.from.head.firstName,
        msg.from.head.lastName,
        msg.chat.id,
        msg.messageId,
        msg.from.head.id
      )
    }
    val telegramSender = TelegramSender(telegramChatDetails)
    val request = UserRequest(msg.text, telegramSender, routingKeyT)
    publisherActor ! request
    Future()
  }

}
