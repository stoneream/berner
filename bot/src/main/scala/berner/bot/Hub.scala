package berner.bot

import berner.database.{HubMessageMappingReader, HubMessageMappingWriter}
import berner.logging.Logger
import berner.model.hub.HubMessageMapping
import club.minnced.discord.webhook.external.JDAWebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Mentions
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

  private def templating(sanitizedContent: String, messageLink: String, urls: String): String = {
    if (urls.isEmpty) {
      // 画像添付がないケース
      s"""
         |$sanitizedContent
         |($messageLink)
         |""".stripMargin
    } else {
      // 画像添付があるケース
      s"""
         |$sanitizedContent
         |$urls
         |($messageLink)
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
        content = {
          val rawContent = sourceMessage.getContentRaw
          val mentions = sourceMessage.getMentions
          val sanitizedContent = sanitizeContent(rawContent, mentions)
          val messageLink = sourceMessage.getJumpUrl

          // 添付画像
          val urls = sourceMessage.getAttachments.asScala
            .map { attachment =>
              if (attachment.isSpoiler) {
                s"||${attachment.getUrl}||"
              } else {
                attachment.getUrl
              }
            }
            .mkString("\n")

          templating(sanitizedContent, messageLink, urls)
        }
        webhookMessage = {
          new WebhookMessageBuilder()
            .setContent(content)
            .setUsername(sourceMessage.getAuthor.getName)
            .setAvatarUrl(sourceMessage.getAuthor.getAvatarUrl)
            .build()
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
        content = {
          val rawContent = sourceMessage.getContentRaw
          val mentions = sourceMessage.getMentions
          val sanitizedContent = sanitizeContent(rawContent, mentions)
          val messageLink = sourceMessage.getJumpUrl
          // 画像添付があるケース
          val urls = sourceMessage.getAttachments.asScala
            .map { attachment =>
              if (attachment.isSpoiler) {
                s"||${attachment.getUrl}||"
              } else {
                attachment.getUrl
              }
            }
            .mkString("\n")

          templating(sanitizedContent, messageLink, urls)
        }
        webhookMessage = {
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
        HubMessageMappingReader.findForUpdate(
          sourceGuildMessageChannelId = guildMessageChannel.getId,
          sourceThreadMessageChannelId = threadChannelOpt.map(_.getId),
          sourceMessageId = event.getMessageId,
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
    } yield {
      allCatch.either(webhookClient.delete(hmm.hubMessageId).get()) match {
        case Left(e) => warn("メッセージの削除に失敗しました", e) // TODO 消し漏らしを検知 & 削除する仕組みがほしい
        case Right(_) =>
          DB.localTx { session =>
            val now = OffsetDateTime.now()
            HubMessageMappingWriter.delete(hmm.id, now)(session)
          }
      }
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

    for {
      category <- extractCategoryName(guildMessageChannel.getName)
      hubMessageMappings = threadChannelOpt.fold {
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
      hubChannel <- sourceGuild.getTextChannelsByName(s"hub-$category", true).asScala.headOption
    } yield {
      hubMessageMappings.foreach { hmm =>
        // 途中で例外が起きるとループが落ちる
        // 例外をキャッチして処理を続行する
        allCatch.either {
          val id = hmm.hubMessageId

          hubChannel.deleteMessageById(id).complete()

          val now = OffsetDateTime.now()
          DB localTx { session => HubMessageMappingWriter.delete(hmm.id, now)(session) }
        } match {
          case Left(e) => warn("メッセージの削除に失敗しました", e)
          case Right(_) => // do nothing
        }
      }
    }
  }
}
