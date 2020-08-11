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

  trait DataRequest
  case class GetUserAccount(botResponse: BotResponse)
  case class GetUser(login: String) extends DataRequest
  case class GetUserRepos(botResponse: BotResponse)
  case class GetRepos(login: String) extends DataRequest
  case class GetCNJoke(botResponse: BotResponse)
  case class GetJoke() extends DataRequest
  case class GetWeatherInCity(city: String) extends DataRequest
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
      fetchData(requestMakerActor,
                GetRepos(user.botResponse.response.getOrElse("")),
                user.botResponse)
    case joke: GetCNJoke =>
      fetchData(requestMakerActor, GetJoke(), joke.botResponse)
    case weather: GetWeather =>
      fetchData(requestMakerActor,
                GetWeatherInCity(weather.botResponse.response.getOrElse("")),
                weather.botResponse)
  }

  def fetchData(requestMakerActor: ActorRef,
                dataRequest: DataRequest,
                response: BotResponse): Unit = {
    (requestMakerActor ? dataRequest).onComplete {
      case Success(value) =>
        val message = createResponseMessage(response, Option(value.toString))
        publisherActor ! message
      case Failure(exception) =>
        val message =
          createResponseMessage(response, Option(exception.getMessage))
        publisherActor ! message
    }
  }

  def createResponseMessage(usersMessage: BotResponse,
                            response: Option[String]): BotResponse = {
    BotResponse(response, usersMessage.sender, usersMessage.replyTo)
  }
}
