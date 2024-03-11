package berner.bot

import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal

class Archiver extends ListenerAdapter {
  private val slashCommandName = "archive"
  private val modalCustomId = "berner-archiver"
  private val modalDownloadPasswordId = "download_password"

  override def onGuildReady(event: GuildReadyEvent): Unit = {
    event.getGuild.upsertCommand(slashCommandName, "Archive").queue()
  }

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != slashCommandName) {
      event.deferReply().queue()
    } else {
      val downloadPassword = TextInput
        .create(modalDownloadPasswordId, "Download Password", TextInputStyle.SHORT)
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
    if (event.getModalId != modalCustomId) {
      event.deferReply().queue()
    } else {
      val downloadPassword = event.getValue(modalDownloadPasswordId).getAsString
      if (downloadPassword.isEmpty) {
        event.deferReply().queue()
      } else {
        // TODO IMPL アーカイブ処理
        event.reply("archiving...").queue()
      }
    }
  }

}
