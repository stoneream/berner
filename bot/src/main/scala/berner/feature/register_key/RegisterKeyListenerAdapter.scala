package berner.feature.register_key

import berner.database.UserPublicKeyWriter
import berner.logging.Logger
import berner.model.register_key.UserPublicKey
import berner.model.register_key.UserPublicKey.UserPublicKeyType
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.text.{TextInput, TextInputStyle}
import net.dv8tion.jda.api.interactions.modals.Modal
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import scalikejdbc.DB

import java.io.StringReader
import java.time.OffsetDateTime
import scala.util.Using
import scala.util.control.Exception.allCatch

class RegisterKeyListenerAdapter extends ListenerAdapter with Logger {
  private val modalCustomId = "berner-register-key"
  private val modalKeyPem = "key-pem"

  override def onSlashCommandInteraction(event: SlashCommandInteractionEvent): Unit = {
    if (event.getName != RegisterKeyListenerAdapter.slashCommandName) {
      // do nothing
      event.deferReply().queue()
    } else {
      val inputKeyPem = TextInput
        .create(modalKeyPem, "PEM", TextInputStyle.PARAGRAPH)
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
      event.deferReply().queue()
    } else {
      val input = for {
        keyPem <- Option(event.getValue(modalKeyPem)).map(_.getAsString)
      } yield {
        keyPem
      }
      input match {
        case Some(keyPem) if keyPem.nonEmpty =>
          // 鍵のチェック
          val publicKeyOpt = Using.resource(new PEMParser(new StringReader(keyPem))) { parser =>
            Option(parser.readObject()) match {
              case Some(info: SubjectPublicKeyInfo) =>
                allCatch.either(Option(new JcaPEMKeyConverter().getPublicKey(info))) match {
                  case Left(e) =>
                    warn("公開鍵のチェックに失敗しました。", e)
                    None
                  case Right(None) =>
                    warn("公開鍵のチェックに失敗しました。")
                    None
                  case Right(Some(publicKey)) => Some(publicKey)
                }
              case _ =>
                warn("公開鍵のチェックに失敗しました。")
                None
            }
          }
          val keyTypeOpt = publicKeyOpt.flatMap { publicKey =>
            // サポートしているのはRSAおよびED25519のみ
            UserPublicKeyType.fromString(publicKey.getAlgorithm)
          }

          // 鍵を登録する
          val keyOpt = for {
            publicKey <- publicKeyOpt
            keyType <- keyTypeOpt
          } yield (publicKey, keyType)

          keyOpt match {
            case Some((_, keyType)) =>
              val now = OffsetDateTime.now()
              val upk = UserPublicKey(
                id = 0L,
                userId = event.getUser.getId,
                keyPem = keyPem,
                keyType = keyType.value,
                createdAt = now,
                updatedAt = now,
                deletedAt = None
              )
              DB localTx { session =>
                UserPublicKeyWriter.write(upk :: Nil)(session)
              }
              event.reply("鍵の登録が完了しました。").setEphemeral(true).queue()
            case None =>
              event.reply("未サポートの鍵形式です。(RSA, ED25519)").setEphemeral(true).queue()
          }
        case _ =>
          event.reply("PEMが入力されていません。").setEphemeral(true).queue()
      }
    }
  }
}

object RegisterKeyListenerAdapter {
  val slashCommandName = "register-key"
  val slashCommandDescription = "Register your public key"
}
