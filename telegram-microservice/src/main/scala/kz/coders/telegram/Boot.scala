package kz.coders.telegram

import akka.actor.{ActorRef, ActorSystem}
import kz.coders.telegram.actors.{AmqpListenerActor, AmqpPublisherActor}
import com.typesafe.config.ConfigFactory
import kz.amqp.{AmpqConsumer, RabbitMqConnection}
import scala.util.{Failure, Success}

object Boot extends App {
  val system = ActorSystem("telegram-demo")
  val config = ConfigFactory.load()
  val host = config.getString("rabbitmq.host")
  val port = config.getInt("rabbitmq.port")
  val username = config.getString("rabbitmq.username")
  val password = config.getString("rabbitmq.password")
  val virtualHost = config.getString("rabbitmq.virtualHost")
  val token = config.getString("application.token")
  val exchangeChatGatewayIn = config.getString("rabbitmq.exchangeChatGatewayIn")
  val exchangeChatGatewayOut =
    config.getString("rabbitmq.exchangeChatGatewayOut")
  val routingKeyIn = config.getString("rabbitmq.routingKeyIn")
  val telegramQueue = config.getString("rabbitmq.telegramQueue")
  val routingKeyTelegram = config.getString("rabbitmq.routingKeyT")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    username,
    password,
    host,
    port,
    virtualHost
  )
  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, exchangeChatGatewayOut, "topic") match {
    case Success(_) =>
      system.log.info("successfully declared telegram exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    telegramQueue,
    exchangeChatGatewayOut,
    routingKeyTelegram
  )

  val ref: ActorRef = system.actorOf(
    AmqpPublisherActor.props(channel, exchangeChatGatewayIn, routingKeyIn))
  val telegramService = new TelegramService(token, ref, routingKeyTelegram)
  val listenerActor = system.actorOf(AmqpListenerActor.props(telegramService))
  channel.basicConsume(telegramQueue, AmpqConsumer(listenerActor))

  telegramService.run()
}
