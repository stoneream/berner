package berner.bot.handler.hub.message

import berner.bot.model.ApplicationContext
import berner.bot.service.HubMessageService
import berner.core.discord.DiscordWebhookClient
import cats.data.ReaderT
import cats.effect.IO
import discord.payload.MessageUpdate
import discord.BotContext
import io.circe.Json
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import scala.util.control.Exception.allCatch

object HubMessageUpdateHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.get.flatMap { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          val botConfig = botContext.config
          json
            .as[MessageUpdate.Data]
            .toOption
            .map { d =>
              hubMessageService.find(d.id, d.channelId, d.guildId).flatMap {
                case Some(hubMessage) =>
                  for {
                    sanitizedContent <- sanitizeContent(d.content, d.mentions)
                    text = {
                      // templating
                      val messageLink = s"https://discord.com/channels/${d.guildId}/${d.channelId}/${d.id}"

                      if (d.attachments.isEmpty) {
                        s"""
                           |$sanitizedContent
                           |($messageLink)
                           |""".stripMargin
                      } else {
                        val urls = d.attachments.map(_.url).mkString("\n")
                        s"""
                           |$sanitizedContent
                           |$urls
                           |($messageLink)
                           |""".stripMargin
                      }
                    }
                    _ <- DiscordWebhookClient.editMessage(hubMessage.messageId, text)(botConfig.timesHubWebhookId, botConfig.timesHubWebhookToken)
                  } yield ()
                case None => IO.unit
              }
            }
            .getOrElse(IO.raiseError(new RuntimeException("Unexpected Json")))

        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }
  }

  private def sanitizeContent(content: String, mentions: Seq[MessageUpdate.Mention]): IO[String] = {
    val mentionsDict = mentions.map(m => (m.id, m.globalName)).toMap

    // ユーザー名を置換
    // 二重にメンションが飛ばないことを目的としている
    val sanitizedContent = "<@(\\d+)>".r
      .replaceAllIn(
        content,
        m => {
          allCatch.either(m.group(1)) match {
            case Right(userId) =>
              mentionsDict.get(userId) match {
                case Some(globalName) => s"@.$globalName"
                case None => "@.unknown"
              }
            case Left(_) => "@.unknown"
          }
        }
      )
      .replace("@here", "@.here")
      .replace("@everyone", "@.everyone")

    IO.pure(sanitizedContent)
  }

}
