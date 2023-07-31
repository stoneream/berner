package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe._
import io.circe.optics.JsonPath._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.{Headers, Method, Request, Uri, UrlForm}
import org.http4s.jdkhttpclient.JdkHttpClient
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
                case Some(content) => {
                  if (content.contains("ping")) {
                    root.d.channel_id.string.getOption(json) match {
                      case Some(channelId) =>
                        for {
                          httpClient <- JdkHttpClient.simple[IO]
                          request = Request[IO](
                            method = Method.POST,
                            uri = Uri.unsafeFromString(s"https://discord.com/api/v10/channels/$channelId/messages"),
                            headers = Headers(
                              "Authorization" -> s"Bot ${ctx.token}"
                            )
                          ).withEntity(
                            UrlForm(("content", "pong"))
                          )
                          response <- httpClient.expect[Json](request)
                        } yield {
                          ctx
                        }
                      case None =>
                        logger.info(s"channelId is not found") *> IO.pure(ctx)
                    }
                  } else {
                    IO.pure(ctx)
                  }
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
