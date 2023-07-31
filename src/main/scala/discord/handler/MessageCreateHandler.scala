package discord.handler

import cats.effect.IO
import discord.{BotContext, DiscordApiClient}
import io.circe._
import io.circe.optics.JsonPath._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MessageCreateHandler {
  private val logger = Slf4jLogger.getLogger[IO]

  def handle(json: Json)(context: BotContext): IO[BotContext] = {

    context match {
      case ctx: BotContext.ReadyBotContext =>
        root.d.author.id.string.getOption(json) match {
          case Some(value) =>
            if (value != ctx.userId) {
              root.d.content.string.getOption(json) match {
                case Some(content) =>
                  if (content.contains("ping")) {
                    root.d.channel_id.string.getOption(json) match {
                      case Some(channelId) =>
                        for {
                          _ <- DiscordApiClient.createMessage("pong", channelId)(ctx.token)
                        } yield {
                          ctx
                        }
                      case None =>
                        logger.info(s"channelId is not found") *> IO.pure(ctx)
                    }
                  } else {
                    IO.pure(ctx)
                  }
                case None => IO.pure(ctx)
              }
            } else {
              IO.pure(ctx)
            }
          case None => IO.pure(ctx)
        }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }
}
