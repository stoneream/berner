package berner.bot

import io.circe._
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.{GuildMessageChannel, MessageChannel}
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload

import java.nio.charset.Charset
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

class Archiver extends ListenerAdapter {
  private val slashCommandName = "archive"
  private val modalCustomId = "berner-archiver"
  private val modalDownloadPasswordId = "download_password"

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

  override def onGuildReady(event: GuildReadyEvent): Unit = {
    event.getGuild.upsertCommand(slashCommandName, "Archive").queue()
  }

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != slashCommandName) {
      event.deferReply().queue()
    } else {
      val downloadPassword = TextInput
        .create(modalDownloadPasswordId, "Download Password (debug)", TextInputStyle.SHORT)
        .setRequired(false)
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
    // メッセージ全件取得
    def fetchHistory(targetChannel: MessageChannel): Seq[Message] = {
      @tailrec
      def g(afterMessageId: String, ls: List[Message]): List[Message] = {
        val history = targetChannel.getHistoryAfter(afterMessageId, 100).complete()
        val messages = history.getRetrievedHistory.asScala.toList

        messages.reverse match {
          case last :: _ => g(last.getId, messages ::: ls)
          case Nil => ls
        }
      }

      val history = targetChannel.getHistoryFromBeginning(100).complete()
      val messages = history.getRetrievedHistory.asScala.toList

      messages.reverse match {
        case last :: _ => g(last.getId, messages)
        case Nil => Nil
      }
    }

    if (event.getModalId == modalCustomId) {
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
          "edited_at" -> fromString(message.getTimeEdited.toString)
        )
      }

      // アップロード制限を超える可能性がある
      // そのため外部のストレージに保存 -> ダウンロードリンクを返す
      // 暗号化して保存したい...
      val data = fromValues(messages).noSpacesSortKeys.getBytes(Charset.forName("UTF8"))
      val file = FileUpload.fromData(data, "archive.json")

      targetChannel.sendFiles(file).setContent("archived!!").queue()
    } else {
      // unknown modal or invalid form
      // do nothing
      event.deferReply().queue()
    }
  }
}
