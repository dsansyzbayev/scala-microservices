package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorSystem}
import kz.coders.chat.gateway.APIs.CNJoker.getRandomCNFact
import kz.coders.chat.gateway.APIs.Githuber.getUserRepositories
import kz.coders.chat.gateway.APIs.Weather.getWeatherByCity
import kz.coders.chat.gateway.actors.DataFetcherActor._
import org.json4s.DefaultFormats
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

class RequestMakerActor(implicit system: ActorSystem) extends Actor {
  implicit val executionContext: ExecutionContextExecutor = context.dispatcher
  implicit val formats: DefaultFormats = DefaultFormats

  override def receive: Receive = {
    case user: GetRepos =>
      val sender = context.sender()
      getUserRepositories(s"https://api.github.com/users/${user.login}/repos")
        .onComplete {
          case Success(value) =>
            val message = value
              .map(
                x =>
                  s"${x.name} has size ${x.size}, stars: ${x.stargazers_count}, ${if (x.fork) "forked" else "not forked"}"
              )
            sender ! message.mkString("\n\n")
          case Failure(exception) =>
            sender ! exception.getMessage
        }
    case weatherInCity: GetWeatherInCity =>
      val sender = context.sender()
      getWeatherByCity(weatherInCity.city).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(exception) =>
          sender ! exception.getMessage
      }
    case _: GetJoke =>
      val sender = context.sender()
      getRandomCNFact.onComplete {
        case Success(value) =>
          sender ! value.joke
        case Failure(exception) =>
          sender ! exception.getMessage
      }

  }

}
