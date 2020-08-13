package kz.coders.chat.gateway

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import kz.amqp.{AmpqConsumer, RabbitMqConnection}
import kz.coders.chat.gateway.actors.{
  AmqpListenerActor,
  AmqpPublisherActor,
  DataFetcherActor,
  DialogflowActor
}

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
  val exchangeChatGatewayIn = config.getString("rabbitmq.exchangeChatGatewayIn")
  val exchangeChatGatewayOut =
    config.getString("rabbitmq.exchangeChatGatewayOut")
  val dialogflowCredentials = config.getString("dialogFlow.credentials")
  val routingKeyIn = config.getString("rabbitmq.routingKeyIn")
  val routingKeyOut = config.getString("rabbitmq.routingKeyOut")
  val gatewayQueue = config.getString("rabbitmq.gatewayQueue")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    username,
    password,
    host,
    port,
    virtualHost
  )

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, exchangeChatGatewayIn, "topic") match {
    case Success(_) =>
      system.log.info(s"succesfully declared exchange $exchangeChatGatewayIn")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    gatewayQueue,
    exchangeChatGatewayIn,
    routingKeyIn
  )

  val publisherActor =
    system.actorOf(AmqpPublisherActor.props(channel, exchangeChatGatewayOut))
  val dataFetcherActor = system.actorOf(DataFetcherActor.props(publisherActor))
  val dialogflowActor =
    system.actorOf(
      DialogflowActor
        .props(dialogflowCredentials, publisherActor, dataFetcherActor))
  val listenerActor =
    system.actorOf(AmqpListenerActor.props(dialogflowActor))
  channel.basicConsume(gatewayQueue, AmpqConsumer(listenerActor))

}
