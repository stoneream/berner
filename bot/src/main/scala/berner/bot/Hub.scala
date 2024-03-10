package berner.bot

import berner.database.{HubMessageReader, HubMessageWriter}
import berner.model.hub.HubMessage
import club.minnced.discord.webhook.external.JDAWebhookClient
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.entities.Mentions
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.message.{GenericMessageEvent, MessageDeleteEvent, MessageReceivedEvent, MessageUpdateEvent}
import net.dv8tion.jda.api.hooks.ListenerAdapter
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._
import scala.util.control.Exception._

class Hub extends ListenerAdapter {

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

  private def resolveParentTextChannel(genericMessageEvent: GenericMessageEvent): Option[GuildMessageChannel] = {
    allCatch
      .opt(genericMessageEvent.getChannel.asThreadChannel())
      .fold {
        allCatch.opt(genericMessageEvent.getChannel.asGuildMessageChannel())
      } { threadChannel => // スレッドの場合は元のチャンネルを取得する
        allCatch.opt(threadChannel.getParentMessageChannel.asStandardGuildMessageChannel())
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
      val sourceMessage = event.getMessage

      resolveParentTextChannel(event)
        .foreach { parentTextChannel =>
          extractCategoryName(parentTextChannel.getName).map { category =>
            val guildChannels = event.getGuild.getChannels.asScala.toList
            val guildMessageChannels = guildChannels.collect { case c: TextChannel => c }

            guildMessageChannels.find(_.getName == s"hub-$category") match {
              case Some(hubChannel) =>
                val content = {
                  val rawContent = sourceMessage.getContentRaw
                  val mentions = sourceMessage.getMentions
                  val sanitizedContent = sanitizeContent(rawContent, mentions)
                  val messageLink = sourceMessage.getJumpUrl
                  // 画像添付があるケース
                  val urls = sourceMessage.getAttachments.asScala.map(_.getUrl).mkString("\n")

                  templating(sanitizedContent, messageLink, urls)
                }

                // webhookがなかったら作成する
                val webhooks = event.getGuild.retrieveWebhooks().complete().asScala
                val webhookName = s"berner-hub-${category}"
                val webHookClient = {
                  val jdaWebHook = webhooks
                    .find { webhook =>
                      webhook.getChannel.getId == hubChannel.getId &&
                      webhook.getName == webhookName
                    }
                    .getOrElse {
                      hubChannel.createWebhook(webhookName).complete()
                    }

                  allCatch.opt(JDAWebhookClient.from(jdaWebHook))
                }

                // メッセージ送信
                val webhookMessage = new WebhookMessageBuilder()
                  .setContent(content)
                  .setUsername(sourceMessage.getAuthor.getName)
                  .setAvatarUrl(sourceMessage.getAuthor.getAvatarUrl)
                  .build()

                webHookClient
                  .flatMap { webhookClient =>
                    allCatch.opt(webhookClient.send(webhookMessage).get())
                  }
                  .map { hubMessage =>
                    // DBに記録
                    val now = OffsetDateTime.now()
                    DB localTx { session =>
                      HubMessageWriter.write(
                        HubMessage(
                          id = 0,
                          sourceMessageId = sourceMessage.getId,
                          sourceChannelId = sourceMessage.getChannel.getId,
                          messageId = s"${hubMessage.getId}",
                          channelId = s"${hubChannel.getId}",
                          guildId = event.getGuild.getId,
                          createdAt = now,
                          updatedAt = now,
                          deletedAt = None
                        )
                      )(session)
                    }
                  }
              case _ => // do nothing
            }
          }
        }
    }

  }

  override def onMessageUpdate(event: MessageUpdateEvent): Unit = {
    if (event.getAuthor.isBot) {
      // do nothing
    } else {
      val sourceMessage = event.getMessage

      DB.readOnly { session =>
        HubMessageReader.find(
          sourceMessageId = sourceMessage.getId,
          sourceChannelId = sourceMessage.getChannel.getId,
          guildId = sourceMessage.getGuild.getId
        )(session)
      }.foreach { hubMessage =>
        resolveParentTextChannel(event)
          .foreach { parentTextChannel =>
            extractCategoryName(parentTextChannel.getName).map { category =>
              val webhooks = event.getGuild.retrieveWebhooks().complete().asScala
              val webhookName = s"berner-hub-${category}"
              val webHookClient = webhooks
                .find { webhook =>
                  webhook.getChannel.getId == hubMessage.channelId &&
                  webhook.getName == webhookName
                }
                .flatMap { jdaWebHook =>
                  allCatch.opt(JDAWebhookClient.from(jdaWebHook))
                }

              val content = {
                val rawContent = sourceMessage.getContentRaw
                val mentions = sourceMessage.getMentions
                val sanitizedContent = sanitizeContent(rawContent, mentions)
                val messageLink = sourceMessage.getJumpUrl
                // 画像添付があるケース
                val urls = sourceMessage.getAttachments.asScala.map(_.getUrl).mkString("\n")

                templating(sanitizedContent, messageLink, urls)
              }

              // メッセージ送信
              val webhookMessage = new WebhookMessageBuilder()
                .setContent(content)
                .setUsername(sourceMessage.getAuthor.getName)
                .setAvatarUrl(sourceMessage.getAuthor.getAvatarUrl)
                .build()

              webHookClient.map { webhookClient =>
                allCatch.opt(webhookClient.edit(hubMessage.messageId, webhookMessage).get())
              }
            }
          }
      }
    }
  }

  override def onMessageDelete(event: MessageDeleteEvent): Unit = {
    resolveParentTextChannel(event)
      .foreach { parentTextChannel =>
        extractCategoryName(parentTextChannel.getName).map { category =>
          DB localTx { session =>
            HubMessageReader
              .findForUpdate(
                sourceMessageId = event.getMessageId,
                sourceChannelId = event.getChannel.getId,
                guildId = event.getGuild.getId
              )(session)
              .foreach { hubMessage =>
                // webhookの解決
                val webhooks = event.getGuild.retrieveWebhooks().complete().asScala
                val webhookName = s"berner-hub-${category}"
                webhooks
                  .find { webhook =>
                    webhook.getChannel.getId == hubMessage.channelId &&
                    webhook.getName == webhookName
                  }
                  .flatMap { jdaWebHook =>
                    allCatch.opt(JDAWebhookClient.from(jdaWebHook))
                  }
                  .map { webHookClient =>
                    // メッセージの削除
                    webHookClient.delete(hubMessage.messageId).get()
                    HubMessageWriter.delete(hubMessage.id, OffsetDateTime.now())(session)
                  }
              }
          }
        }
      }
  }
}
