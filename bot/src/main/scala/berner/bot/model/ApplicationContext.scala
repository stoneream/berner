package berner.bot.model

import berner.bot.handler.hub.HubContext
import cats.data.ReaderT
import cats.effect.IO
import cats.effect.std.AtomicCell

case class ApplicationContext(
    discordBotContext: discord.BotContext,
    hubContext: HubContext
)

object ApplicationContext {
  type ApplicationContextState = AtomicCell[IO, ApplicationContext]

  type Handler[T] = ReaderT[IO, ApplicationContextState, T]
}
