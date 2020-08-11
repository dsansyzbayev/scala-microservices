import actors.{AmqpListenerActor, AmqpPublisherActor}
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import http.routes.AllRoutes
import kz.amqp.{RabbitMqConnection, AmpqConsumer}
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("http-microservice")
  implicit val materializer: Materializer = Materializer(system)
  implicit val ex: ExecutionContext = system.dispatcher
  val logger = LoggerFactory.getLogger("")

  val config = ConfigFactory.load()
  val host = config.getString("application.host")
  val port = config.getInt("application.port")
  val rmqHost = config.getString("rabbitmq.host")
  val rmqPort = config.getInt("rabbitmq.port")
  val username = config.getString("rabbitmq.username")
  val password = config.getString("rabbitmq.password")
  val virtualHost = config.getString("rabbitmq.virtualHost")
  val exchangeChatGatewayOut =
    config.getString("rabbitmq.exchangeChatGatewayOut")
  val exchangeChatGatewayIn = config.getString("rabbitmq.exchangeChatGatewayIn")
  val httpQueue = config.getString("rabbitmq.httpQueue")
  val routingKeyIn = config.getString("rabbitmq.routingKeyIn")
  val routingKeyHttp = config.getString("rabbitmq.routingKeyHttp")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    username,
    password,
    rmqHost,
    rmqPort,
    virtualHost
  )

  val channel = connection.createChannel()

  RabbitMqConnection.declareExchange(channel, exchangeChatGatewayOut, "topic") match {
    case Success(_) => system.log.info("successfully declared exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    httpQueue,
    exchangeChatGatewayOut,
    routingKeyHttp
  )

  val publisher: ActorRef = system.actorOf(
    AmqpPublisherActor.props(channel, exchangeChatGatewayIn, routingKeyIn))
  val listener: ActorRef = system.actorOf(AmqpListenerActor.props())
  channel.basicConsume(httpQueue, AmpqConsumer(listener))

  val allRoutes = new AllRoutes(publisher, routingKeyHttp)

  Http().bindAndHandle(allRoutes.handlers, host, port)

  logger.info(s"Runnning on $host:$port")
}
