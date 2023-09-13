package discord.handler

import application.model.HubMessage
import cats.effect.IO
import database.service.HubMessageService
import discord.{BotContext, DiscordApiClient, DiscordWebhookClient}
import io.circe._
import io.circe.optics.JsonPath._

import java.time.LocalDateTime

object MessageCreateHandler {
  def handle(json: Json)(context: BotContext, hubMessageService: HubMessageService[IO]): IO[BotContext] = {

    val guildIdPath = root.d.guild_id.string
    val messageIdPath = root.d.id.string
    val authorUsernamePath = root.d.author.username.string
    val authorUserIdPath = root.d.author.id.string
    val authorAvatarPath = root.d.author.avatar.string
    val contentPath = root.d.content.string
    val channelIdPath = root.d.channel_id.string
    val mentionsPath = root.d.mentions.arr
    val attachmentsPath = root.d.attachments.arr
    val attachmentUrlPath = root.url.string

    context match {
      case context: BotContext.ReadyBotContext =>
        (for {
          guildId <- guildIdPath.getOption(json)
          sourceMessageId <- messageIdPath.getOption(json)
          authorUsername <- authorUsernamePath.getOption(json)
          authorUserId <- authorUserIdPath.getOption(json)
          authorAvatarOpt = authorAvatarPath.getOption(json)
          content <- contentPath.getOption(json)
          sourceChannelId <- channelIdPath.getOption(json)
          mentionsJson <- mentionsPath.getOption(json)
          attachmentsJson <- attachmentsPath.getOption(json)
        } yield {
          // ping-pong
          val pingPong = if (content == "ping") {
            DiscordApiClient.createMessage("pong", sourceChannelId)(context.config.token) *> IO.unit
          } else {
            IO.unit
          }

          // バカハブ
          val hub = {
            val authorIsNotMe = authorUserId != context.meUserId
            val hasTimes = context.times.exists(_.id == sourceChannelId)
            val hasTimesThreads = context.timesThreads.exists(_.id == sourceChannelId)

            val mentions = mentionsJson.flatMap { mentionJson =>
              for {
                globalName <- root.global_name.string.getOption(mentionJson)
                userId <- root.id.string.getOption(mentionJson)
              } yield {
                (userId, globalName)
              }
            }.toMap

            if (authorIsNotMe && (hasTimes || hasTimesThreads)) {
              val sanitizedContent = {
                // メンションが二重に飛ぶので対策
                "<@(\\d+)>".r
                  .replaceAllIn(
                    content,
                    { m =>
                      val userId = m.group(1)
                      mentions.get(userId).map { globalName => s"@.$globalName" }.getOrElse(s"@.$userId")
                    }
                  )
                  .replace("@here", "@.here")
                  .replace("@everyone", "@.everyone")
              }

              val messageLink = s"https://discord.com/channels/$guildId/$sourceChannelId/$sourceMessageId"
              val attachmentUrls = attachmentsJson.flatMap(attachmentUrlPath.getOption)

              val text = if (attachmentUrls.isEmpty) {
                s"""$sanitizedContent
                   |($messageLink)
                   |""".stripMargin
              } else {
                s"""$sanitizedContent
                   |${attachmentUrls.mkString("\n")}
                   |($messageLink)
                   |""".stripMargin
              }

              // https://discord.com/developers/docs/reference#image-formatting
              val avatarUrl = authorAvatarOpt.map { authorAvatar => s"https://cdn.discordapp.com/avatars/$authorUserId/$authorAvatar.png" }
              for {
                response <- DiscordWebhookClient
                  .execute(text, authorUsername, avatarUrl)(context.config.timesHubWebhookId, context.config.timesHubWebhookToken)
                  .map { json =>
                    (for {
                      channelId <- root.channel_id.string.getOption(json)
                      messageId <- root.id.string.getOption(json)
                    } yield (channelId, messageId)).getOrElse(throw new Exception("unexpected response"))
                  }
                (channelId, messageId) = response
                hubMessage = HubMessage(
                  id = 0, // write時に自動採番される
                  sourceMessageId = sourceMessageId,
                  sourceChannelId = sourceChannelId,
                  messageId = messageId,
                  channelId = channelId,
                  guildId = guildId,
                  createdAt = LocalDateTime.now(), // todo https://typelevel.org/cats-effect/docs/typeclasses/clock
                  updatedAt = LocalDateTime.now(),
                  deletedAt = None
                )
                _ <- hubMessageService.write(hubMessage)
              } yield {
                context
              }
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
