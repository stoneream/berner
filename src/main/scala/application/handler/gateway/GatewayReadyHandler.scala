package application.handler.gateway

import application.ApplicationContext
import cats.data.StateT
import cats.effect.IO
import discord.BotContext
import discord.payload.Ready
import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration.snakeCaseTransformation

object GatewayReadyHandler {

  def handle(json: Json): ApplicationContext.Handler[Unit] = StateT { currentState =>
    currentState.discordBotContext match {
      case botContext1: BotContext.Init =>
        json.as[Ready.Data].toOption match {
          case Some(d) =>
            val botContext2 = BotContext.Ready(
              config = botContext1.config,
              times = List.empty,
              timesThreads = List.empty,
              meUserId = d.user.id
            )
            val nextState = currentState.copy(discordBotContext = botContext2)
            IO(nextState, ())
          case _ => ??? // todo unexpected
        }
      case _ => ??? // todo unexpected
    }
  }
}
