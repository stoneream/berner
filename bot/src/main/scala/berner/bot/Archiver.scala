package berner.bot

import berner.bot.Archiver.slashCommandName
import io.circe._
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.{GuildMessageChannel, MessageChannel}
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.{AesKeyStrength, EncryptionMethod}

import java.nio.charset.Charset
import java.nio.file.{Files, StandardOpenOption}
import java.time.OffsetDateTime
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

class Archiver extends ListenerAdapter {
  private val modalCustomId = "berner-archiver"
  private val modalZipPassword = "zip-password"

  private def resolveTextChannel(message: Message): (GuildMessageChannel, Option[ThreadChannel]) = {
    val eventSourceChannel = message.getChannel

    allCatch.opt(eventSourceChannel.asThreadChannel()) match {
      case Some(threadChannel) =>
        // スレッドの場合は親チャンネルを取得
        val parentTextChannel = threadChannel.getParentMessageChannel.asStandardGuildMessageChannel()
        (parentTextChannel, Some(threadChannel))
      case None =>
        (eventSourceChannel.asGuildMessageChannel(), None)
    }
  }

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != slashCommandName) {
      // do nothing
    } else {
      val downloadPassword = TextInput
        .create(modalZipPassword, "Zip Password", TextInputStyle.SHORT)
        .setRequired(true)
        .setMinLength(8)
        .setMaxLength(128)
        .build()

      val modal = Modal
        .create(modalCustomId, "Archiver")
        .addComponents(ActionRow.of(downloadPassword))
        .build()

      event.replyModal(modal).queue()
    }
  }

  override def onModalInteraction(event: ModalInteractionEvent): Unit = {
    // TODO メッセージ件数が多すぎる場合にOOMを起こす可能性がありゅ
    // メッセージ全件取得
    def fetchHistory(targetChannel: MessageChannel): Seq[Message] = {
      @tailrec
      def g(afterMessageId: String, ls: List[Message]): List[Message] = {
        val history = targetChannel.getHistoryAfter(afterMessageId, 100).complete()
        val messages = history.getRetrievedHistory.asScala.toList
        val headOption = messages.headOption

        headOption match {
          case Some(head) =>
            g(head.getId, ls ::: messages)
          case None => ls
        }
      }

      val history = targetChannel.getHistoryFromBeginning(100).complete()
      val messages = history.getRetrievedHistory.asScala.toList
      val lastOption = messages.lastOption

      lastOption.map { last => g(last.getId, messages) }.getOrElse(Nil)
    }

    if (event.getModalId != modalCustomId) {
      // unknown modal
      // do nothing
      event.deferReply().queue()
    } else {
      Option(event.getValue(modalZipPassword))
        .map(_.getAsString)
        .fold {
          // invalid form
          // do nothing
          event.deferReply().queue()
        } { zipPassword =>
          event.reply("archiving...").queue()

          // TODO 一般のテキストチャンネルの場合は直下のスレッドも含めてアーカイブする
          val targetChannel = event.getChannel

          import Json._

          val messages = fetchHistory(targetChannel).map { message =>
            val guild = message.getGuild
            val (guildMessageChannel, threadChannelOpt) = resolveTextChannel(message)
            val author = message.getAuthor
            val attachments = message.getAttachments
            val createdAt = message.getTimeCreated

            // 横着気味
            obj(
              "guild_id" -> fromString(guild.getId),
              "guild_name" -> fromString(guild.getName),
              "channel_id" -> fromString(guildMessageChannel.getId),
              "channel_name" -> fromString(guildMessageChannel.getName),
              "thread_channel_id" -> threadChannelOpt.map(_.getId).map(fromString).getOrElse(Json.Null),
              "thread_channel_name" -> threadChannelOpt.map(_.getName).map(fromString).getOrElse(Json.Null),
              "author_id" -> fromString(author.getId),
              "author_name" -> fromString(author.getName),
              "author_global_name" -> Option(author.getGlobalName).map(fromString).getOrElse(Json.Null),
              "author_avatar_url" -> Option(author.getAvatarUrl).map(fromString).getOrElse(Json.Null),
              "content" -> fromString(message.getContentRaw),
              "attachments" -> fromValues(attachments.asScala.map(a => fromString(a.getUrl)).toList),
              "created_at" -> fromString(createdAt.toString),
              "edited_at" -> Option(message.getTimeEdited).map(d => fromString(d.toString)).getOrElse(Json.Null)
            )
          }

          // TODO アップロード制限を超える可能性があるため対策したい
          // そのため外部のストレージに保存 -> ダウンロードリンクを返す みたいな形にしたい

          val data = fromValues(messages).noSpaces

          for {
            tempFile <- allCatch.opt(Files.createTempFile("berner-archive-", ".zip.temp"))
            zos <- allCatch.opt {
              val os = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
              new ZipOutputStream(os, zipPassword.toCharArray, Charset.forName("UTF-8"))
            }
          } yield {
            try {
              zos.putNextEntry(new ZipParameters() {
                setEncryptFiles(true)
                setEncryptionMethod(EncryptionMethod.AES)
                setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256)
                setFileNameInZip("messages.json")
              })
              zos.write(data.getBytes(Charset.forName("UTF-8")))
              zos.closeEntry()
            } finally {
              zos.close()
            }

            val now = OffsetDateTime.now.toEpochSecond
            val filename = s"archive-$now.zip"
            val file = FileUpload.fromData(tempFile, filename)

            targetChannel.sendFiles(file).setContent("archived!!").complete()
          }
        }
    }
  }
}

object Archiver {
  val slashCommandName = "archiver"
  val slashCommandDescription = "Archive Channel Messages"
}
