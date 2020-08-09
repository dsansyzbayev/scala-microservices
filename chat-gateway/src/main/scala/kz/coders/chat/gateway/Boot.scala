package kz.coders.chat.gateway

import akka.actor.{ActorSystem, Props}
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import kz.coders.chat.gateway.actors.{
  AmqpListenerActor,
  AmqpPublisherActor,
  DataFetcherActor,
  DialogflowActor
}
import amqp.{AmpqConsumer, RabbitMqConnection}
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("chat-gateway")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ex: ExecutionContext = system.dispatcher
  val config = ConfigFactory.load()
  val host = config.getString("rabbitmq.host")
  val port = config.getInt("rabbitmq.port")
  val username = config.getString("rabbitmq.username")
  val password = config.getString("rabbitmq.password")
  val virtualHost = config.getString("rabbitmq.virtualHost")
  val exchangeChatGateway = config.getString("rabbitmq.exchangeChatGateway")
  val exchangeTelegram = config.getString("rabbitmq.exchangeTelegram")
  val exchangeHttp = config.getString("rabbitmq.exchangeHttp")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    username,
    password,
    host,
    port,
    virtualHost
  )

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, exchangeChatGateway, "topic") match {
    case Success(_) => system.log.info("succesfully declared exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, exchangeTelegram, "topic") match {
    case Success(_) => system.log.info("succesfully declared telegram exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, exchangeHttp, "topic") match {
    case Success(_) => system.log.info("succesfully declared http exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    "Q:gateway-queue",
    "X:chat-gateway",
    "user.chat.message"
  )

  val publisherActor = system.actorOf(AmqpPublisherActor.props(channel))
  val dialogflowActor = system.actorOf(Props(new DialogflowActor()))

  val dataFetcherActor = system.actorOf(DataFetcherActor.props(publisherActor))
  val listenerActor =
    system.actorOf(
      AmqpListenerActor
        .props(publisherActor, dialogflowActor, dataFetcherActor))
  channel.basicConsume("Q:gateway-queue", AmpqConsumer(listenerActor))

}
