package berner.feature.register_key

import berner.model.register_key.UserPublicKey.UserPublicKeyType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.{SelectMenu, SelectOption, StringSelectMenu}
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal

class RegisterKeyListenerAdapter extends ListenerAdapter {
  private val modalCustomId = "berner-register-key"
  private val modalKeyType = "key-type"
  private val modalKeyPem = "key-pem"

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != RegisterKeyListenerAdapter.slashCommandName) {
      // do nothing
      event.deferReply().queue()
    } else {
      val selectKeyType = StringSelectMenu
        .create(modalKeyType)
        .addOption("RSA", "rsa")
        .addOption("ECDSA", "ecdsa")
        .setRequiredRange(1, 1)
        .build()

      val inputKeyPem = TextInput
        .create(modalKeyPem, "PEM", TextInputStyle.PARAGRAPH)
        .setRequired(true)
        .setMinLength(1)
        .build()

      val modal = Modal
        .create(modalCustomId, "Register Key")
        .addComponents(ActionRow.of(selectKeyType))
        .addComponents(ActionRow.of(inputKeyPem))
        .build()

      event.replyModal(modal).queue()
    }
  }

  override def onModalInteraction(event: ModalInteractionEvent): Unit = {
    if (event.getModalId != modalCustomId) {
      // do nothing
      event.deferReply().queue()
    } else {
      val input = for {
        keyType <- Option(event.getValue(modalKeyType)).map(_.getAsString).flatMap(UserPublicKeyType.fromString)
        keyPem <- Option(event.getValue(modalKeyPem)).map(_.getAsString)
      } yield {
        (keyType, keyPem)
      }

      input match {
        case Some((keyType, keyPem)) =>
          // 鍵のフォーマットをチェック
          // 鍵を登録する
          // すでに登録されている鍵がある場合は上書き
          // 登録完了メッセージを送信
          ???
        case None =>
          // 鍵のタイプが不正もしくはPEMが入力されていない
          // エラーメッセージを送信
          ???
      }
    }
  }
}

object RegisterKeyListenerAdapter {
  val slashCommandName = "register-key"
  val slashCommandDescription = "Register your public key"
}
