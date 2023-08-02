package discord.handler

import cats.effect.IO
import discord.{BotContext, DiscordApiClient, DiscordWebhookClient}
import io.circe._
import io.circe.optics.JsonPath._
import org.typelevel.log4cats.slf4j.Slf4jLogger

object MessageCreateHandler {
  private val logger = Slf4jLogger.getLogger[IO]

  def handle(json: Json)(context: BotContext): IO[BotContext] = {

    val guildIdPath = root.d.guild_id.string
    val messageIdPath = root.d.id.string
    val authorUsernamePath = root.d.author.username.string
    val authorUserIdPath = root.d.author.id.string
    val authorAvatarPath = root.d.author.avatar.string
    val contentPath = root.d.content.string
    val channelIdPath = root.d.channel_id.string
    val mentionsPath = root.d.mentions.arr

    context match {
      case context: BotContext.ReadyBotContext =>
        (for {
          guildId <- guildIdPath.getOption(json)
          messageId <- messageIdPath.getOption(json)
          authorUsername <- authorUsernamePath.getOption(json)
          authorUserId <- authorUserIdPath.getOption(json)
          authorAvatar <- authorAvatarPath.getOption(json)
          content <- contentPath.getOption(json)
          channelId <- channelIdPath.getOption(json)
          mentionsJson <- mentionsPath.getOption(json)
        } yield {
          // ping-pong
          val pingPong = if (content == "ping") {
            DiscordApiClient.createMessage("pong", channelId)(context.config.token) *> IO.unit
          } else {
            IO.unit
          }

          // バカハブ
          val hub = {
            val authorIsNotMe = authorUserId != context.meUserId
            val monitorTarget = context.times.exists(_.id == channelId)

            val mentions = mentionsJson.flatMap { mentionJson =>
              for {
                globalName <- root.global_name.string.getOption(mentionJson)
                userId <- root.id.string.getOption(mentionJson)
              } yield {
                (userId, globalName)
              }
            }.toMap

            if (authorIsNotMe && monitorTarget) {
              // メンションが二重に飛ぶので対策
              val sanitizedContent = {
                "<@(\\d+)>".r.replaceAllIn(
                  content,
                  { m =>
                    val userId = m.group(1)
                    mentions.get(userId).map { globalName => s"@.$globalName" }.getOrElse(s"@.$userId")
                  }
                )
              }

              val messageLink = s"https://discord.com/channels/$guildId/$channelId/$messageId"

              val text =
                s"""
                   |$sanitizedContent ($messageLink)
                   |""".stripMargin

              // https://discord.com/developers/docs/reference#image-formatting
              val avatarUrl = s"https://cdn.discordapp.com/avatars/$authorUserId/$authorAvatar.png"

              DiscordWebhookClient.execute(
                text,
                authorUsername,
                avatarUrl
              )(
                context.config.timesHubWebhookId,
                context.config.timesHubWebhookToken
              ) *> IO.pure(context)
            } else {
              IO.pure(context)
            }
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
