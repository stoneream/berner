package application.handler.ping

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO
import discord.payload.MessageCreate
import discord.{BotContext, DiscordApiClient}
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.auto._

object PingHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.get.map { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          val botConfig = botContext.config
          json.as[MessageCreate.Data].toOption.map { d =>
            if (d.content == "ping") {
              for {
                _ <- DiscordApiClient.createMessage("pong", d.channelId)(botConfig.token)
              } yield ()
            } else {
              // do nothing
              IO.unit
            }
          }
      }
    }
  }
}
