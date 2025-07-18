package berner.feature.register_key

import berner.feature.register_key.RegisterKeyLogic.KeyParseError
import berner.logging.Logger
import database.UserPublicKey
import database.extension.UserPublicKeyExtension
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal
import scalikejdbc.DB

import java.time.OffsetDateTime

class RegisterKeyListenerAdapter extends ListenerAdapter with Logger {
  private val modalCustomId = "berner-register-key"
  private val modalKey = "key"

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != RegisterKeyListenerAdapter.slashCommandName) {
      // do nothing
    } else {
      val inputKeyPem = TextInput
        .create(modalKey, "PEM/OpenSSH", TextInputStyle.PARAGRAPH)
        .setRequired(true)
        .setMinLength(1)
        .build()

      val modal = Modal
        .create(modalCustomId, "Register Key")
        .addComponents(ActionRow.of(inputKeyPem))
        .build()

      event.replyModal(modal).queue()
    }
  }

  override def onModalInteraction(event: ModalInteractionEvent): Unit = {
    if (event.getModalId != modalCustomId) {
      // do nothing
    } else {
      val input = for {
        key <- Option(event.getValue(modalKey)).map(_.getAsString)
      } yield {
        key
      }
      input match {
        case Some(key) if key.nonEmpty =>
          RegisterKeyLogic.detectKeyType(key).flatMap { keyType => // 鍵のフォーマットを判定する
            RegisterKeyLogic.parseKey(key, keyType).map { case (publicKey, algorithm) => // 鍵をパースする
              (keyType, algorithm, publicKey)
            }
          } match {
            case Left(e) =>
              e match {
                case KeyParseError.InvalidFormat => event.reply("公開鍵のフォーマットが不正です。").setEphemeral(true).queue()
                case KeyParseError.UnsupportedFormat => event.reply("未サポートの鍵フォーマットです。(PEM, OpenSSH)").setEphemeral(true).queue()
                case KeyParseError.UnsupportedType => event.reply("未サポートの鍵アルゴリズムです。(RSA, ED25519)").setEphemeral(true).queue()
              }
            case Right((keyType, algorithm, _)) =>
              DB localTx { implicit session =>
                val now = OffsetDateTime.now()
                // すでに存在する古い鍵は削除する
                UserPublicKeyExtension.deleteByUserId(event.getUser.getId, now)(session)

                // 新しい鍵を登録
                val upk = UserPublicKey(
                  id = 0L,
                  userId = event.getUser.getId,
                  keyValue = key,
                  keyAlgorithm = algorithm.value,
                  keyType = keyType.value,
                  createdAt = now,
                  updatedAt = now,
                  deletedAt = None
                )
                UserPublicKeyExtension.write(upk :: Nil)(session)
              }

              event.reply("公開鍵の登録が完了しました。").setEphemeral(true).queue()
          }
        case _ =>
          event.reply("公開鍵が入力されていません。").setEphemeral(true).queue()
      }
    }
  }
}

object RegisterKeyListenerAdapter {
  val slashCommandName = "register-key"
  val slashCommandDescription = "Register your public key"
}
