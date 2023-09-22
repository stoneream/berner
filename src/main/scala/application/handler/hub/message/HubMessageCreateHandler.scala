package application.handler.hub.message

import application.ApplicationContext
import application.model.HubMessage
import cats.data.ReaderT
import cats.effect.IO
import database.service.HubMessageService
import discord.payload.MessageCreate
import discord.{BotContext, DiscordWebhookClient}
import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration

import java.time.LocalDateTime
import scala.util.control.Exception._

object HubMessageCreateHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.get.map { context =>
      val hubContext = context.hubContext
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          val botConfig = botContext.config
          json
            .as[MessageCreate.Data]
            .toOption
            .map { d =>
              val authorIsMe = d.author.id == botContext.me.id
              val hasTimes = hubContext.timesChannels.contains(d.channelId)
              val hasTimesThreads = hubContext.timesThreads.contains(d.channelId)

              if (!authorIsMe && (hasTimes || hasTimesThreads)) {

                for {
                  sanitizedContent <- sanitizeContent(d.content, d.mentions)
                  text = {
                    // templating
                    val messageLink = s"https://discord.com/channels/${d.guildId}/${d.channelId}/${d.id}"
                    s"""
                       |$sanitizedContent
                       |($messageLink)
                       |""".stripMargin
                  }
                  postHubResult <- postHub(d, text)(botConfig.timesHubWebhookId, botConfig.timesHubWebhookToken)
                  (hubChannelId, hubMessageId) = postHubResult
                  _ <- writeHubMessage(d.id, d.channelId, hubMessageId, hubChannelId, d.guildId)(hubMessageService)
                } yield ()
              } else {
                // do nothing
                IO.unit
              }
            }
            .getOrElse(IO.raiseError(new RuntimeException("Unexpected Json")))
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }.void
  }

  private def sanitizeContent(content: String, mentions: Seq[MessageCreate.Mention]): IO[String] = {
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

  private def postHub(data: MessageCreate.Data, text: String)(webhookId: String, webhookToken: String): IO[(String, String)] = {
    import io.circe.optics.JsonPath._

    // https://discord.com/developers/docs/reference#image-formatting
    val avatarUrl = s"https://cdn.discordapp.com/avatars/${data.author.id}/${data.author.avatar}.png"

    DiscordWebhookClient.execute(text, data.author.username, Some(avatarUrl))(webhookId, webhookToken).flatMap { json =>
      val result = for {
        channelId <- root.channel_id.string.getOption(json)
        messageId <- root.id.string.getOption(json)
      } yield (channelId, messageId)

      result.fold[IO[(String, String)]](IO.raiseError(new RuntimeException("unexpected response")))(IO.pure)
    }
  }

  private def writeHubMessage(
      sourceMessageId: String,
      sourceChannelId: String,
      hubMessageId: String,
      hubChannelId: String,
      guildId: String
  )(hubMessageService: HubMessageService[IO]): IO[Unit] = {
    val now = LocalDateTime.now()

    val hubMessage = HubMessage(
      id = 0, // write時に自動採番される
      sourceMessageId = sourceMessageId,
      sourceChannelId = sourceChannelId,
      messageId = hubMessageId,
      channelId = hubChannelId,
      guildId = guildId,
      createdAt = now, // todo https://typelevel.org/cats-effect/docs/typeclasses/clock
      updatedAt = now,
      deletedAt = None
    )

    hubMessageService.write(hubMessage).void
  }

}
