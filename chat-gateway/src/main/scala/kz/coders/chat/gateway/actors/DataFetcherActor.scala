package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.DataFetcherActor._
import kz.domain.library.messages.BotResponse
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object DataFetcherActor {
  def props(publisherActor: ActorRef)(implicit system: ActorSystem): Props =
    Props(new DataFetcherActor(publisherActor))

  case class GetUserAccount(botResponse: BotResponse)
  case class GetUser(login: String)
  case class GetUserRepos(botResponse: BotResponse)
  case class GetRepos(login: String)
  case class GetCNJoke(botResponse: BotResponse)
  case class GetJoke()
  case class GetWeatherInCity(city: String)
  case class GetWeather(botResponse: BotResponse)
}

class DataFetcherActor(publisherActor: ActorRef)(implicit system: ActorSystem)
    extends Actor {
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher
  val requestMakerActor: ActorRef =
    system.actorOf(Props(new RequestMakerActor()))

  override def receive: Receive = {
    case user: GetUserAccount =>
      val message = user.botResponse.response.head
      (requestMakerActor ? GetRepos(message)).onComplete {
        case Success(value) =>
          val message =
            createResponseMessage(user.botResponse, Option(value.toString))
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(user.botResponse,
                                  Option(exception.getMessage))
          sendToPublisher(publisherActor, message)
      }
    case joke: GetCNJoke =>
      (requestMakerActor ? GetJoke()).onComplete {
        case Success(value) =>
          val message =
            createResponseMessage(joke.botResponse, Option(value.toString))
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(joke.botResponse,
                                  Option(exception.getMessage))
          sendToPublisher(publisherActor, message)
      }
    case weather: GetWeather =>
      val city = weather.botResponse.response.head
      (requestMakerActor ? GetWeatherInCity(city)).onComplete {
        case Success(value) =>
          val message =
            createResponseMessage(weather.botResponse, Option(value.toString))
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(weather.botResponse,
                                  Option(exception.getMessage))
          sendToPublisher(publisherActor, message)
      }
  }

  def sendToPublisher(publisher: ActorRef, message: BotResponse): Unit = {
    publisher ! message
  }

  def createResponseMessage(usersMessage: BotResponse,
                            response: Option[String]): BotResponse = {
    BotResponse(response, usersMessage.sender, usersMessage.replyTo)
  }
}
