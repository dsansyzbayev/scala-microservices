package kz.coders.telegram

import akka.actor.{ActorRef, ActorSystem, Props}
import kz.coders.telegram.actors.{AmqpListenerActor, AmqpPublisherActor}
import kz.coders.telegram.amqp.{AmqpConsumer, RabbitMqConnection}
import com.typesafe.config.ConfigFactory

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
  val exchangeTelegram = config.getString("rabbitmq.exchangeTelegram")
  val exchangeChatGateway = config.getString("rabbitmq.exchangeChatGateway")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    username,
    password,
    host,
    port,
    virtualHost
  )
  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, exchangeChatGateway, "topic") match {
    case Success(_) => system.log.info("successfully declared exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(channel, exchangeTelegram, "topic") match {
    case Success(_) =>
      system.log.info("successfully declared telegram exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    "Q:telegram-queue",
    "X:telegram-microservice",
    "user.chat.message"
  )

  val ref: ActorRef = system.actorOf(AmqpPublisherActor.props(channel))
  val telegramService = new TelegramService(token, ref)
  val listenerActor = system.actorOf(AmqpListenerActor.props(telegramService))
  channel.basicConsume("Q:telegram-queue", AmqpConsumer(listenerActor))

  telegramService.run()
}
