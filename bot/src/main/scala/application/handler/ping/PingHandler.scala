package application.handler.ping

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO
import discord.payload.MessageCreate
import discord.{BotContext, DiscordApiClient}
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._

object PingHandler {

  def handle(json: Json): ApplicationContext.Handler[Unit] = ReaderT { state =>
    for {
      context <- state.get
      _ <- {
        context.discordBotContext match {
          case botContext: BotContext.Ready =>
            val botConfig = botContext.config
            for {
              d <- asMessageCreateData(json)
              _ <- {
                if (d.content == "ping") {
                  DiscordApiClient.createMessage("pong", d.channelId)(botConfig.token) >> IO.unit
                } else {
                  // do nothing
                  IO.unit
                }
              }
            } yield ()
          case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
        }
      }
    } yield {}
  }

  private def asMessageCreateData(json: Json): IO[MessageCreate.Data] = {
    implicit val config = Configuration.default.withSnakeCaseMemberNames

    json.as[MessageCreate.Data] match {
      case Left(e) => IO.raiseError(new RuntimeException("Unexpected Json", e))
      case Right(value) => IO.pure(value)
    }
  }
}
