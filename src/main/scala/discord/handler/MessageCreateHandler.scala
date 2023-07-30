package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
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
                case Some(content) =>
                  val mentionPattern = "<@\\d+>".r
                  val filteredMentionContent = mentionPattern.replaceAllIn(content, "")

                  if (filteredMentionContent.contains("ping")) {
                    root.d.channel_id.string.getOption(json) match {
                      case Some(channelId) =>
                        JdkHttpClient
                          .simple[IO]
                          .map { client =>
                            client
                              .run(
                                Request[IO](
                                  method = Method.POST,
                                  uri = Uri.unsafeFromString(s"https://discord.com/api/v10/channels/$channelId/messages"),
                                  headers = Headers(
                                    "Authorization" -> s"Bot ${ctx.token}"
                                  )
                                ).withEntity(
                                  UrlForm(("content", "pong"))
                                )
                              )
                              .use { response =>
                                logger.info(s"response : ${response.status}")
                              }
                              .handleErrorWith { err =>
                                logger.error(err)(s"failed to send message")
                              }
                          }
                          .map { _ =>
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
