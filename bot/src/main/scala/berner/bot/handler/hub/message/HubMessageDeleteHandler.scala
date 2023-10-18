package berner.bot.handler.hub.message

import berner.bot.model.ApplicationContext
import berner.bot.service.HubMessageService
import berner.core.discord.DiscordApiClient
import cats.data.ReaderT
import cats.effect.IO
import discord.payload.MessageDelete
import discord.BotContext
import io.circe.Json
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import java.time.LocalDateTime

object HubMessageDeleteHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.get.flatMap { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          for {
            d <- asMessageDeleteData(json)
            hubMessage <- hubMessageService.find(d.id, d.channelId, d.guildId)
            _ <- hubMessage match {
              case Some(hubMessage) =>
                for {
                  _ <- DiscordApiClient.deleteMessage(hubMessage.channelId, hubMessage.messageId)(botContext.config.token)
                  _ <- hubMessageService.delete(hubMessage.id, LocalDateTime.now())
                } yield ()
              case None => IO.unit
            }
          } yield ()
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }
  }

  private def asMessageDeleteData(json: Json): IO[MessageDelete.Data] = {
    implicit val config = Configuration.default.withSnakeCaseMemberNames

    json.as[MessageDelete.Data] match {
      case Left(e) => IO.raiseError(new RuntimeException("Unexpected Json", e))
      case Right(value) => IO.pure(value)
    }
  }

}
