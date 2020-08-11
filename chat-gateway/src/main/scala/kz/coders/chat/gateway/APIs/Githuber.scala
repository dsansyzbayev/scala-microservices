package kz.coders.chat.gateway.APIs

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import kz.coders.chat.gateway.util.RestClientImpl.get
import scala.concurrent.{ExecutionContext, Future}

object Githuber {
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  case class GithubUser(login: String, public_repos: String)
  case class GithubRepository(name: String,
                              size: Int,
                              fork: Boolean,
                              stargazers_count: Int)

  def getGithubUser(username: String)(
      implicit system: ActorSystem,
      materializer: Materializer,
      ex: ExecutionContext
  ): Future[GithubUser] = {
    val response = get(s"https://api.github.com/users/$username")
    response.map { body =>
      parse(body).extract[GithubUser]
    }
  }

  def getUserRepositories(repoUrl: String)(
      implicit system: ActorSystem,
      materializer: Materializer,
      ex: ExecutionContext
  ): Future[List[GithubRepository]] = {
    val response = get(repoUrl)
    response.map { body =>
      parse(body).extract[List[GithubRepository]]
    }
  }

}
