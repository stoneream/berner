package discord.handler

import cats.effect.IO
import discord.{BotContext, DiscordApiClient}
import io.circe._
import io.circe.optics.JsonPath._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MessageCreateHandler {
  private val logger = Slf4jLogger.getLogger[IO]

  def handle(json: Json)(context: BotContext): IO[BotContext] = {

    val authorUserIdPath = root.d.author.id.string
    val contentPath = root.d.content.string
    val channelIdPath = root.d.channel_id.string

    context match {
      case context: BotContext.ReadyBotContext =>
        (for {
          authorUserId <- authorUserIdPath.getOption(json)
          content <- contentPath.getOption(json)
          channelId <- channelIdPath.getOption(json)
        } yield {
          // ping-pong
          val pingPong = if (content == "ping") {
            DiscordApiClient.createMessage("pong", channelId)(context.token) *> IO.unit
          } else {
            IO.unit
          }

          // バカハブ
          val authorIsNotMe = authorUserId != context.meUserId
          val monitorTarget = context.times.exists(_.id == channelId)

          val hub = context.hubTimes match {
            case Some(hubTimes) =>
              if (authorIsNotMe && monitorTarget) {
                // todo サニタイズ
                DiscordApiClient.createMessage(content, hubTimes.id)(context.token) *> IO.pure(context)
              } else {
                IO.pure(context)
              }
            case _ => IO.pure(context)
          }

          pingPong *> hub
        }).getOrElse {
          // do nothing
          IO.pure(context)
        }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }
}
