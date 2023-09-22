package application.handler.hub.message

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO
import database.service.HubMessageService
import discord.payload.MessageDelete
import discord.{BotContext, DiscordApiClient}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration

import java.time.LocalDateTime

object HubMessageDeleteHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.get.map { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          json.as[MessageDelete.Data].map { d =>
            hubMessageService.find(d.id, d.channelId, d.guildId).map {
              case Some(hubMessage) =>
                for {
                  _ <- DiscordApiClient.deleteMessage(hubMessage.channelId, hubMessage.messageId)(botContext.config.token)
                  _ <- hubMessageService.delete(hubMessage.id, LocalDateTime.now())
                } yield ()
              case None => IO.unit
            }
          }
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }.void
  }

}
