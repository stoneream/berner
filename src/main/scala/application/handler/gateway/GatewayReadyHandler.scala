package application.handler.gateway

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO
import discord.BotContext
import discord.payload.Ready
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._

object GatewayReadyHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.evalGetAndUpdate { context =>
      context.discordBotContext match {
        case botContext: BotContext.Init =>
          json.as[Ready.Data].toOption match {
            case Some(d) =>
              val botContext2 = BotContext.Ready(
                config = botContext.config,
                me = d.user
              )
              val next = context.copy(discordBotContext = botContext2)
              IO(next)
            case _ => IO.raiseError(new RuntimeException("Unexpected Json"))
          }
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }.void
  }
}
