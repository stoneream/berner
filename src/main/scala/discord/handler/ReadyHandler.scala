package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath.root

object ReadyHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {
    context match {
      case BotContext.InitializedBotContext(config) =>
        val meUserIdPath = root.d.user.id.string
        meUserIdPath.getOption(json) match {
          case Some(meUserId) =>
            val nextContext = BotContext.ReadyBotContext(
              config = config,
              times = List.empty,
              timesThreads = List.empty,
              meUserId = meUserId
            )
            IO.pure(nextContext)
          case None => IO.raiseError(new Exception("failed to get user id"))
        }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }

}
