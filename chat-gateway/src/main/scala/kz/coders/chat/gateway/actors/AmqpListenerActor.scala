package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.DataFetcherActor._
import kz.domain.library.messages.UsersMessage
import org.json4s.jackson.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AmqpListenerActor {
  def props(publisherActor: ActorRef,
            dialogflowActor: ActorRef,
            dataFetcherActor: ActorRef)(implicit system: ActorSystem): Props =
    Props(
      new AmqpListenerActor(publisherActor, dialogflowActor, dataFetcherActor))
}

class AmqpListenerActor(
    publisherActor: ActorRef,
    dialogflowActor: ActorRef,
    dataFetcherActor: ActorRef)(implicit system: ActorSystem)
    extends Actor
    with ActorLogging {
  implicit val formats: Formats = DefaultFormats
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case msg: String =>
      log.info(s"received message $msg")
      val userMessage = parse(msg).extract[UsersMessage]

      log.info(s"Message received from ${userMessage.message.head}")

      (dialogflowActor ? userMessage.message.head).onComplete {
        case Success(value) =>
          log.info(s"DialogFlow value $value")
          val dialogflowResponse = value.asInstanceOf[DialogflowResponse]
          val response = Option(dialogflowResponse.response)
          val intent = dialogflowResponse.intent
          val message = UsersMessage(userMessage.senderPlatform,
                                     response,
                                     intent,
                                     userMessage.args)

          intent match {
            case "get-github-account-details" =>
              dataFetcherActor ! GetUserAccount(message)
            case "Jokes-intent" =>
              dataFetcherActor ! GetCNJoke(message)
            case "weather-intent" =>
              dataFetcherActor ! GetWeather(message)
            case "no-response" =>
              publisherActor ! message
            case _ =>
              publisherActor ! message
          }
        case Failure(exception) => log.warning(exception.getMessage)
      }

  }

}
