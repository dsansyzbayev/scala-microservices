package kz.coders.chat.gateway.actors

import java.io.FileInputStream
import java.util.UUID
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.google.cloud.dialogflow.v2.{
  DetectIntentRequest,
  QueryInput,
  SessionName,
  SessionsClient,
  SessionsSettings,
  TextInput
}
import kz.coders.chat.gateway.actors.DataFetcherActor.{
  GetCNJoke,
  GetUserAccount,
  GetWeather
}
import kz.domain.library.messages.{BotResponse, UserRequest}

object DialogflowActor {
  def props(dialogFlowCredentials: String,
            publisherActor: ActorRef,
            dataFetcherActor: ActorRef): Props =
    Props(
      new DialogflowActor(dialogFlowCredentials,
                          publisherActor,
                          dataFetcherActor))
}

class DialogflowActor(dialogFlowCredentials: String,
                      publisherActor: ActorRef,
                      dataFetcherActor: ActorRef)
    extends Actor
    with ActorLogging {
  val credentials: GoogleCredentials =
    GoogleCredentials.fromStream(new FileInputStream(dialogFlowCredentials))

  val projectId: String =
    credentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val client: SessionsClient = SessionsClient
    .create(
      SessionsSettings
        .newBuilder()
        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
        .build()
    )

  val session: SessionName = SessionName.of(projectId, UUID.randomUUID().toString)

  override def receive: Receive = {
    case message: UserRequest =>
      val response = client
        .detectIntent(
          DetectIntentRequest
            .newBuilder()
            .setQueryInput(
              QueryInput
                .newBuilder()
                .setText(
                  TextInput
                    .newBuilder()
                    .setText(message.message.getOrElse(""))
                    .setLanguageCode("RU-RU")
                    .build())
                .build())
            .setSession(session.toString)
            .build()
        )
        .getQueryResult

      response.getIntent.getDisplayName match {
        case "get-github-account-details" =>
          val params = response.getParameters.getFieldsMap
            .get("github-account")
            .getStringValue
          val botResponse =
            BotResponse(Option(params), message.sender, message.replyTo)
          dataFetcherActor ! GetUserAccount(botResponse)
        case "Jokes-intent" =>
          val botResponse =
            BotResponse(Option(""), message.sender, message.replyTo)
          dataFetcherActor ! GetCNJoke(botResponse)
        case "weather-intent" =>
          val params = response.getParameters.getFieldsMap
            .get("city")
            .getStringValue
          val botResponse =
            BotResponse(Option(params), message.sender, message.replyTo)
          dataFetcherActor ! GetWeather(botResponse)
        case _ =>
          val botResponse = BotResponse(Option(response.getFulfillmentText),
                                        message.sender,
                                        message.replyTo)
          publisherActor ! botResponse
      }

  }

}
