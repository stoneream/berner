package application.handler.hub.message

import application.ApplicationContext
import application.handler.hub.HubContext
import application.model.HubMessage
import cats.data.ReaderT
import cats.effect.IO
import database.service.HubMessageService
import discord.payload.MessageCreate
import discord.{BotContext, DiscordWebhookClient}
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._

import java.time.LocalDateTime
import scala.util.control.Exception._

object HubMessageCreateHandler {
  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    for {
      context <- state.get
      hubContext = context.hubContext
      _ <- context.discordBotContext match {
        case botContext: BotContext.Ready =>
          val botConfig = botContext.config
          asMessageCreateData(json).flatMap { d =>
            isTargetMessage(d, botContext, hubContext).flatMap { isTarget =>
              if (isTarget) {
                for {
                  sanitizedContent <- sanitizeContent(d.content, d.mentions)
                  text <- templating(sanitizedContent, d)
                  postHubResult <- postHub(d, text)(botConfig.timesHubWebhookId, botConfig.timesHubWebhookToken)
                  (hubChannelId, hubMessageId) = postHubResult
                  _ <- writeHubMessage(d.id, d.channelId, hubMessageId, hubChannelId, d.guildId)(hubMessageService)
                } yield ()
              } else {
                // do nothing
                IO.unit
              }.map(_ => {})
            }
          }
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    } yield ()
  }

  private def asMessageCreateData(json: Json): IO[MessageCreate.Data] = {
    implicit val config = Configuration.default.withSnakeCaseMemberNames

    json.as[MessageCreate.Data] match {
      case Left(e) => IO.raiseError(new RuntimeException("Unexpected Json", e))
      case Right(value) => IO.pure(value)
    }
  }

  private def isTargetMessage(data: MessageCreate.Data, botContext: BotContext.Ready, hubContext: HubContext): IO[Boolean] = {
    val authorIsMe = data.author.id == botContext.me.id
    val hasTimes = hubContext.timesChannels.contains(data.channelId)
    val hasTimesThreads = hubContext.timesThreads.contains(data.channelId)
    val result = !authorIsMe && (hasTimes || hasTimesThreads)

    IO.pure(result)
  }

  private def templating(text: String, d: MessageCreate.Data): IO[String] = {
    // templating
    val messageLink = s"https://discord.com/channels/${d.guildId}/${d.channelId}/${d.id}"

    val message = if (d.attachments.isEmpty) {
      s"""
         |$text
         |($messageLink)
         |""".stripMargin
    } else {
      val urls = d.attachments.map(_.url).mkString("\n")
      s"""
         |$text
         |$urls
         |($messageLink)
         |""".stripMargin
    }

    IO.pure(message)
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

    val avatarUrl = data.author.avatar.map { avatar =>
      s"https://cdn.discordapp.com/avatars/${data.author.id}/$avatar.png"
    }

    DiscordWebhookClient.execute(text, data.author.username, avatarUrl)(webhookId, webhookToken).flatMap { json =>
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
