package kz.coders.chat.gateway.APIs

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import kz.coders.chat.gateway.util.RestClientImpl.get
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse

import scala.concurrent.{ExecutionContext, Future}

object Weather {
  implicit val defaultFormats: DefaultFormats.type = DefaultFormats

  case class WeatherResponse(weather: List[WeatherResponseBody],
                             main: MainResponseBody)
  case class WeatherResponseBody(id: Int, main: String, description: String)
  case class MainResponseBody(temp: Int, feels_like: Int)

  val config = ConfigFactory.load()
  val apiKey = config.getString("application.weatherApiKey")

  def getWeatherByCity(city: String)(implicit system: ActorSystem,
                                     materializer: Materializer,
                                     ex: ExecutionContext): Future[String] = {
    val url =
      s"https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric"
    val response = get(url)
    response.map { body =>
      val weather = parse(body).extract[WeatherResponse]
      val response =
        s"Weather in $city.\n ${weather.weather.map(x => x.main).mkString("")}, ${weather.weather
          .map(x => x.description)
          .mkString("")}, temp: ${weather.main.temp}, feels like: ${weather.main.feels_like}"
      response
    }
  }
}
