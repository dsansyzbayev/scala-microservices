package kz.coders.chat.gateway.APIs

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import kz.coders.chat.gateway.util.RestClientImpl.get
import scala.concurrent.{ExecutionContext, Future}

object CNJoker {
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats
  case class JokeResponse(`type`: String, value: JokeResponseBody)
  case class JokeResponseBody(id: Int, joke: String)
  case class ChuckNorrisJoke(joke: String)

  def getRandomCNFact(implicit system: ActorSystem,
                      materializer: Materializer,
                      ex: ExecutionContext): Future[ChuckNorrisJoke] = {
    val response = get("http://api.icndb.com/jokes/random")
    response.map { body =>
      ChuckNorrisJoke(parse(body).extract[JokeResponse].value.joke)
    }
  }
}