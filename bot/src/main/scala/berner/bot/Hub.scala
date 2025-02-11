package berner.bot

import berner.database.{HubMessageDeleteQueueWriter, HubMessageMappingReader, HubMessageMappingWriter}
import berner.logging.Logger
import berner.model.hub.{HubMessageDeleteQueue, HubMessageMapping}
import club.minnced.discord.webhook.external.JDAWebhookClient
import club.minnced.discord.webhook.send.{WebhookEmbedBuilder, WebhookMessageBuilder}
import net.dv8tion.jda.api.entities.Mentions
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
import net.dv8tion.jda.api.events.message.{GenericMessageEvent, MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.api.hooks.ListenerAdapter
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._
import scala.util.control.Exception._

class Hub extends ListenerAdapter with Logger {

  private def sanitizeContent(rawContent: String, mentions: Mentions): String = {
    val mentionedUser = mentions.getUsers.asScala.toList
    val mentionsMap = mentionedUser.map(m => (m.getId, m.getGlobalName)).toMap

    // ユーザー名を置換
    // 二重にメンションが飛ばないことを目的としている
    val sanitizedContent = "<@(\\d+)>".r
      .replaceAllIn(
        rawContent,
        m => {
          allCatch.either(m.group(1)) match {
            case Right(userId) =>
              mentionsMap.get(userId) match {
                case Some(globalName) => s"@.$globalName"
                case None => "@.unknown"
              }
            case Left(_) => "@.unknown"
          }
        }
      )
      .replace("@here", "@.here")
      .replace("@everyone", "@.everyone")

    sanitizedContent
  }

  private def templating(
      sanitizedContent: String,
      attachments: List[Attachment],
      messageLink: String,
      forwarded: Boolean = false
  ): String = {
    val urls = attachments
      .map { attachment =>
        // 添付画像のネタバレ防止を考慮
        if (attachment.isSpoiler) {
          s"||${attachment.getUrl}||"
        } else {
          attachment.getUrl
        }
      }
      .mkString("\n")

    val jumpLinkWithInfo =
      if (forwarded) {
        s"($messageLink, Forwarded)"
      } else {
        s"($messageLink)"
      }

    if (urls.isEmpty) {
      // 画像添付がないケース
      s"""
         |$sanitizedContent
         |$jumpLinkWithInfo
         |""".stripMargin
    } else {
      // 画像添付があるケース
      s"""
         |$sanitizedContent
         |$urls
         |$jumpLinkWithInfo
         |""".stripMargin
    }
  }

  private def resolveTextChannel(genericMessageEvent: GenericMessageEvent): (GuildMessageChannel, Option[ThreadChannel]) = {
    val eventSourceChannel = genericMessageEvent.getChannel

    allCatch.opt(eventSourceChannel.asThreadChannel()) match {
      case Some(threadChannel) =>
        // スレッドの場合は親チャンネルを取得
        val parentTextChannel = threadChannel.getParentMessageChannel.asStandardGuildMessageChannel()
        (parentTextChannel, Some(threadChannel))
      case None =>
        (eventSourceChannel.asGuildMessageChannel(), None)
    }
  }

  private def extractCategoryName(channelName: String): Option[String] = {
    channelName.split('-').toList match {
      case category :: _ => Some(category)
      case _ => None
    }
  }

  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (event.getAuthor.isBot) {
      // do nothing
    } else {
      val sourceGuild = event.getGuild
      val sourceMessage = event.getMessage
      val (guildMessageChannel, threadChannelOpt) = resolveTextChannel(event)

      for {
        category <- extractCategoryName(guildMessageChannel.getName)
        channelName = s"hub-$category"
        hubChannel <- sourceGuild.getTextChannelsByName(channelName, true).asScala.headOption
        webhook <- allCatch.opt {
          val webhooks = event.getGuild.retrieveWebhooks().complete().asScala
          val webhookName = s"berner-hub-${category}"
          val jdaWebHook = webhooks
            .find { webhook =>
              // webhookを探す
              webhook.getChannel.getId == hubChannel.getId &&
              webhook.getName == webhookName
            }
            .getOrElse {
              // webhookが存在しない場合は新しく作成する
              hubChannel.createWebhook(webhookName).complete()
            }

          JDAWebhookClient.from(jdaWebHook)
        }
        webhookMessage = {
          val sourceAuthor = sourceMessage.getAuthor
          sourceMessage.getMessageSnapshots.asScala.toList match {
            case Nil => // 通常のメッセージ
              val rawContent = sourceMessage.getContentRaw
              val mentions = sourceMessage.getMentions
              val sanitizedContent = sanitizeContent(rawContent, mentions)
              val attachments = sourceMessage.getAttachments.asScala
              val content = templating(sanitizedContent, attachments.toList, sourceMessage.getJumpUrl)

              new WebhookMessageBuilder()
                .setContent(content)
                .setUsername(sourceAuthor.getName)
                .setAvatarUrl(sourceAuthor.getAvatarUrl)
                .build()
            case head :: _ => // 転送されたメッセージ
              val rawContent = head.getContentRaw
              val mentions = head.getMentions
              val sanitizedContent = sanitizeContent(rawContent, mentions)
              val attachments = head.getAttachments.asScala
              val content = templating(
                sanitizedContent,
                attachments.toList,
                sourceMessage.getJumpUrl,
                true
              )
              val embeds = head.getEmbeds.asScala.map { embed =>
                WebhookEmbedBuilder.fromJDA(embed).build()
              }.asJava

              new WebhookMessageBuilder()
                .setContent(content)
                .setUsername(sourceAuthor.getName)
                .setAvatarUrl(sourceAuthor.getAvatarUrl)
                .addEmbeds(embeds)
                .build()
          }
        }
      } yield {
        allCatch.either(webhook.send(webhookMessage).get()) match {
          case Right(hubMessage) =>
            // DBに記録
            val now = OffsetDateTime.now()
            DB localTx { session =>
              HubMessageMappingWriter.write(
                List(
                  HubMessageMapping(
                    id = 0,
                    guildId = sourceGuild.getId,
                    sourceGuildMessageChannelId = guildMessageChannel.getId,
                    sourceThreadMessageChannelId = threadChannelOpt.map(_.getId),
                    sourceMessageId = sourceMessage.getId,
                    hubGuildMessageChannelId = hubChannel.getId,
                    hubMessageId = hubMessage.getId.toString,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = None
                  )
                )
              )(session)
            }
          case Left(e) => warn("メッセージの送信に失敗しました", e)
        }
      }
    }

  }

  override def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    if (event.getAuthor.isBot) {
      // do nothing
    } else {
      val sourceGuild = event.getGuild
      val sourceMessage = event.getMessage
      val (guildMessageChannel, threadChannelOpt) = resolveTextChannel(event)

      for {
        hmm <- DB.readOnly { session =>
          HubMessageMappingReader.find(
            sourceGuildMessageChannelId = guildMessageChannel.getId,
            sourceThreadMessageChannelId = threadChannelOpt.map(_.getId),
            sourceMessageId = sourceMessage.getId,
            guildId = sourceGuild.getId
          )(session)
        }
        category <- extractCategoryName(guildMessageChannel.getName)
        webhookClient <- allCatch.opt {
          // webhookの解決
          val webhooks = event.getGuild.retrieveWebhooks().complete().asScala
          val webhookName = s"berner-hub-${category}"
          webhooks
            .find { webhook =>
              webhook.getChannel.getId == hmm.hubGuildMessageChannelId &&
              webhook.getName == webhookName
            }
            .map { jdaWebHook => JDAWebhookClient.from(jdaWebHook) }
            .getOrElse(throw new RuntimeException("webhook not found"))
        }
        webhookMessage = {
          val rawContent = sourceMessage.getContentRaw
          val mentions = sourceMessage.getMentions
          val sanitizedContent = sanitizeContent(rawContent, mentions)
          val messageLink = sourceMessage.getJumpUrl
          val attachments = sourceMessage.getAttachments.asScala.toList
          val content = templating(sanitizedContent, attachments, messageLink)

          new WebhookMessageBuilder()
            .setContent(content)
            .setUsername(sourceMessage.getAuthor.getName)
            .setAvatarUrl(sourceMessage.getAuthor.getAvatarUrl)
            .build()
        }
      } yield {
        allCatch.either(webhookClient.edit(hmm.hubMessageId, webhookMessage).get()) match {
          case Left(e) => warn("メッセージの編集に失敗しました", e)
          case Right(_) => // do nothing
        }
      }
    }
  }

  override def onMessageDelete(event: MessageDeleteEvent): Unit = {
    val sourceGuild = event.getGuild
    val (guildMessageChannel, threadChannelOpt) = resolveTextChannel(event)

    for {
      hmm <- DB.readOnly { session =>
        HubMessageMappingReader.find(
          sourceGuildMessageChannelId = guildMessageChannel.getId,
          sourceThreadMessageChannelId = threadChannelOpt.map(_.getId),
          sourceMessageId = event.getMessageId,
          guildId = sourceGuild.getId
        )(session)
      }
    } yield {
      val now = OffsetDateTime.now()
      val row = HubMessageDeleteQueue(
        id = 0,
        hubMessageMappingId = hmm.id,
        status = 0, // pending
        createdAt = now,
        updatedAt = now,
        deletedAt = None
      )

      DB.localTx { session =>
        HubMessageDeleteQueueWriter.write(row :: Nil)(session)
      }

      info("メッセージの削除をキューイングしました。(hub_message_mapping_reader#id: %d)".format(hmm.id))
    }
  }

  override def onChannelDelete(event: ChannelDeleteEvent): Unit = {
    val sourceGuild = event.getGuild
    val (guildMessageChannel, threadChannelOpt) = {
      val eventSourceChannel = event.getChannel

      allCatch.either(eventSourceChannel.asThreadChannel()) match {
        case Left(value) =>
          warn("チャンネルもしくはスレッドが見つかりませんでした", value)
          (eventSourceChannel.asGuildMessageChannel(), None)
        case Right(threadChannel) =>
          // スレッドの場合は親チャンネルを解決
          val parentTextChannel = threadChannel.getParentMessageChannel.asStandardGuildMessageChannel()
          (parentTextChannel, Some(threadChannel))
      }
    }

    val hubMessageMappings = threadChannelOpt.fold {
      // チャンネルを消した場合は直下のスレッドも対象
      DB.readOnly { session =>
        HubMessageMappingReader.find(
          sourceGuildMessageChannelId = guildMessageChannel.getId,
          guildId = sourceGuild.getId
        )(session)
      }
    } { threadChannel =>
      // スレッドを消した場合はスレッドのみ
      DB.readOnly { session =>
        HubMessageMappingReader.find(
          sourceGuildMessageChannelId = guildMessageChannel.getId,
          sourceThreadMessageChannelId = threadChannel.getId,
          guildId = sourceGuild.getId
        )(session)
      }
    }

    val now = OffsetDateTime.now()
    val hmdq = hubMessageMappings.map { hmm =>
      HubMessageDeleteQueue(
        id = 0,
        hubMessageMappingId = hmm.id,
        status = 0, // pending
        createdAt = now,
        updatedAt = now,
        deletedAt = None
      )
    }

    DB.localTx { session =>
      HubMessageDeleteQueueWriter.write(hmdq)(session)
    }

    info("メッセージの削除をキューイングしました。(hub_message_mapping_reader#id: %s)".format(hubMessageMappings.map(_.id).mkString(", ")))
  }
}
