package discord.handler

import cats.effect.IO
import database.service.HubMessageService
import discord.{BotContext, DiscordApiClient, DiscordWebhookClient}
import io.circe._
import io.circe.optics.JsonPath._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.time.LocalDateTime

object MessageUpdateHandler {
  private val logger = Slf4jLogger.getLogger[IO]

  private case class Payload(
      guildId: String,
      messageId: String,
      channelId: String,
      content: String,
      mentions: Map[String, String]
  )

  def handle(json: Json)(context: BotContext, hubMessageService: HubMessageService[IO]): IO[BotContext] = {

    context match {
      case context: BotContext.ReadyBotContext =>
        // バカハブ / 編集時ハンドラ
        // todo パーサーがアホなのでどうにかする
        parse(json).fold {
          IO.pure(context)
        } { payload =>
          hubMessageService.find(payload.messageId, payload.channelId, payload.guildId).flatMap {
            case Some(hubMessage) =>
              val sanitizedContent = {
                // todo サニタイズを共通化
                // メンションが二重に飛ぶので対策
                "<@(\\d+)>".r
                  .replaceAllIn(
                    payload.content,
                    { m =>
                      val userId = m.group(1)
                      payload.mentions.get(userId).map { globalName => s"@.$globalName" }.getOrElse(s"@.$userId")
                    }
                  )
                  .replace("@here", "@.here")
                  .replace("@everyone", "@.everyone")
              }

              val messageLink = s"https://discord.com/channels/${hubMessage.guildId}/${hubMessage.sourceChannelId}/${hubMessage.sourceMessageId}"

              val text =
                s"""
                   |$sanitizedContent ($messageLink)
                   |""".stripMargin

              DiscordWebhookClient.editMessage(hubMessage.messageId, text)(context.config.timesHubWebhookId, context.config.timesHubWebhookToken) *>
                IO.pure(context)

            case None => IO.pure(context)
          }
        }
      case _ => IO.raiseError(new Exception("unexpected bot context"))
    }
  }

  private def parse(json: Json): Option[Payload] = {
    val guildIdPath = root.d.guild_id.string
    val messageIdPath = root.d.id.string
    val channelIdPath = root.d.channel_id.string
    val contentPath = root.d.content.string
    val mentionsPath = root.d.mentions.arr

    (for {
      guildId <- guildIdPath.getOption(json)
      sourceMessageId <- messageIdPath.getOption(json)
      sourceChannelId <- channelIdPath.getOption(json)
      content <- contentPath.getOption(json)
      mentionsJson <- mentionsPath.getOption(json)
      mentions = mentionsJson.flatMap { mentionJson =>
        for {
          globalName <- root.global_name.string.getOption(mentionJson)
          userId <- root.id.string.getOption(mentionJson)
        } yield {
          (userId, globalName)
        }
      }.toMap
    } yield {
      Payload(guildId, sourceMessageId, sourceChannelId, content, mentions)
    })
  }
}