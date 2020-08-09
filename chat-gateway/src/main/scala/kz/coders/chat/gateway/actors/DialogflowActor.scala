package kz.coders.chat.gateway.actors

import java.io.FileInputStream
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
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
case class DialogflowResponse(intent: String, response: String)

class DialogflowActor extends Actor with ActorLogging {
  val credentials = GoogleCredentials.fromStream(
    new FileInputStream(
      "/home/daniyar/one-tech-telegram-bot-kmyb-08beba509cd1.json"))

  val projectId =
    credentials.asInstanceOf[ServiceAccountCredentials].getProjectId

  val client = SessionsClient
    .create(
      SessionsSettings
        .newBuilder()
        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
        .build()
    )

  val session = SessionName.of(projectId, UUID.randomUUID().toString)

  override def receive: Receive = {
    case command: String =>
      val sender = context.sender()
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
                    .setText(command)
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
          sender ! DialogflowResponse(response.getIntent.getDisplayName, params)
        case "Jokes-intent" =>
          sender ! DialogflowResponse(response.getIntent.getDisplayName, "")
        case "weather-intent" =>
          val params = response.getParameters.getFieldsMap
            .get("city")
            .getStringValue
          sender ! DialogflowResponse(response.getIntent.getDisplayName, params)
        case _ =>
          sender ! DialogflowResponse("no-response",
                                      response.getFulfillmentText)
      }

  }

}
