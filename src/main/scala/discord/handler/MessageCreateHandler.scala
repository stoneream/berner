package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath._

object MessageCreateHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {

    context match {
      case _: BotContext.Uninitialized => IO.raiseError(new Exception("bot context is not initialized"))
      case ctx: BotContext.InitializedBotContext =>
        root.d.author.id.string.getOption(json) match {
          case Some(value) =>
            if (value != ctx.userId) {
              root.d.content.string.getOption(json) match {
                case Some(content) => IO.println("YOOOOOOOOOOOOOOOOOOOOOOOOOO") *> IO.pure(ctx) // todo parse content
                case None => IO.pure(ctx)
              }
            } else {
              IO.pure(ctx)
            }
          case None => IO.pure(ctx)
        }
    }
  }

}
