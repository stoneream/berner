package discord.handler

import cats.effect.IO
import database.service.HubMessageService
import discord.{BotContext, DiscordApiClient, DiscordWebhookClient}
import io.circe._
import io.circe.optics.JsonPath._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDateTime

object MessageDeleteHandler {
  private val logger = Slf4jLogger.getLogger[IO]

  private case class Payload(
      guildId: String,
      messageId: String,
      channelId: String
  )

  def handle(json: Json)(context: BotContext, hubMessageService: HubMessageService[IO]): IO[BotContext] = {

    context match {
      case context: BotContext.ReadyBotContext =>
        // バカハブ / 削除時ハンドラ
        val payload = parse(json)
        hubMessageService.find(payload.messageId, payload.channelId, payload.guildId).flatMap {
          case Some(hubMessage) =>
            DiscordApiClient.deleteMessage(hubMessage.channelId, hubMessage.messageId)(context.config.token) *>
              hubMessageService.delete(hubMessage.id, LocalDateTime.now()) *>
              IO.pure(context)
          case None => IO.pure(context)
        }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }

  private def parse(json: Json): Payload = {
    val guildIdPath = root.d.guild_id.string
    val messageIdPath = root.d.id.string
    val channelIdPath = root.d.channel_id.string

    (for {
      guildId <- guildIdPath.getOption(json)
      sourceMessageId <- messageIdPath.getOption(json)
      sourceChannelId <- channelIdPath.getOption(json)
    } yield {
      Payload(guildId, sourceMessageId, sourceChannelId)
    }).getOrElse(throw new Exception("unexpected json"))
  }
}
