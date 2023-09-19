package application

import application.handler.hub.HubContext
import cats.data.{ReaderT, StateT}
import cats.effect.IO

case class ApplicationContext(
    discordBotContext: discord.BotContext,
    hubContext: HubContext
)

object ApplicationContext {
  type Handler[T] = StateT[IO, ApplicationContext, T]
}
