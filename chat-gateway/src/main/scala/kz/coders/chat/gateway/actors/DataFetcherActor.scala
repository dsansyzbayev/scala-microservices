package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.AmqpPublisherActor.TelegramChatDetails
import kz.coders.chat.gateway.actors.DataFetcherActor._
import kz.domain.library.messages.UsersMessage
import org.json4s.{DefaultFormats, jvalue2extractable}
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object DataFetcherActor {
  def props(publisherActor: ActorRef)(implicit system: ActorSystem): Props =
    Props(new DataFetcherActor(publisherActor))

  case class GetUserAccount(usersMessage: UsersMessage)
  case class GetUser(login: String)
  case class GetUserRepos(usersMessage: UsersMessage)
  case class GetRepos(login: String)
  case class GetCNJoke(usersMessage: UsersMessage)
  case class GetJoke()
  case class GetWeatherInCity(city: String)
  case class GetWeather(usersMessage: UsersMessage)
}

class DataFetcherActor(publisherActor: ActorRef)(implicit system: ActorSystem)
    extends Actor {
  implicit val timeout: Timeout = 5.seconds
  implicit val executionContext: ExecutionContext = context.dispatcher
  implicit val formats: DefaultFormats.type = DefaultFormats

  val requestMakerActor: ActorRef =
    system.actorOf(Props(new RequestMakerActor()))

  override def receive: Receive = {
    case user: GetUserAccount =>
      val message = user.usersMessage.message.head
      (requestMakerActor ? GetRepos(message)).onComplete {
        case Success(value) =>
          val message = createResponseMessage(user.usersMessage, value.toString)
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(user.usersMessage, exception.getMessage)
          sendToPublisher(publisherActor, message)
      }
    case joke: GetCNJoke =>
      (requestMakerActor ? GetJoke()).onComplete {
        case Success(value) =>
          val message = createResponseMessage(joke.usersMessage, value.toString)
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(joke.usersMessage, exception.getMessage)
          sendToPublisher(publisherActor, message)
      }
    case weather: GetWeather =>
      val city = weather.usersMessage.message.head
      (requestMakerActor ? GetWeatherInCity(city)).onComplete {
        case Success(value) =>
          val message =
            createResponseMessage(weather.usersMessage, value.toString)
          sendToPublisher(publisherActor, message)
        case Failure(exception) =>
          val message =
            createResponseMessage(weather.usersMessage, exception.getMessage)
          sendToPublisher(publisherActor, message)
      }
  }

  def sendToPublisher(publisher: ActorRef, message: UsersMessage): Unit = {
    publisher ! message
  }

  def createResponseMessage(usersMessage: UsersMessage,
                            response: String): UsersMessage = {
    UsersMessage(usersMessage.senderPlatform,
                 Option(response),
                 "",
                 usersMessage.args)
  }
}
