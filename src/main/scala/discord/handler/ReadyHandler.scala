package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath.root

object ReadyHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {
    context match {
      case BotContext.InitializedBotContext(token) => {
        root.d.user.id.string.getOption(json) match {
          case Some(value) => IO.pure(BotContext.ReadyBotContext(token, value))
          case None => IO.raiseError(new Exception("failed to get user id"))
        }
      }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }

}
