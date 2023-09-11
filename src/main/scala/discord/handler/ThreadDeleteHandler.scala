package discord.handler

import cats.effect.IO
import cats.implicits.toTraverseOps
import database.service.HubMessageService
import discord.{BotContext, DiscordApiClient}
import io.circe.Json
import io.circe.optics.JsonPath.root

import java.time.LocalDateTime

object ThreadDeleteHandler {

  private case class Payload(
      guildId: String,
      sourceThreadId: String
  )

  def handle(json: Json)(context: BotContext, hubMessageService: HubMessageService[IO]): IO[BotContext] = {
    context match {
      case BotContext.InitializedBotContext(_) => IO.raiseError(new Exception("unexpected bot context"))
      case botContext @ BotContext.ReadyBotContext(config, _, timesThreads, _) =>
        val payload = parse(json)

        hubMessageService
          .findBySourceChannelId(payload.sourceThreadId, payload.guildId)
          .flatMap { hubMessages =>
            hubMessages.traverse { hubMessage =>
              for {
                _ <- DiscordApiClient.deleteMessage(hubMessage.channelId, hubMessage.messageId)(config.token)
                _ <- hubMessageService.delete(hubMessage.id, LocalDateTime.now())
              } yield ()
            }
          }
          .as {
            botContext.copy(timesThreads = timesThreads.filterNot(_.id == payload.sourceThreadId))
          }
    }
  }

  private def parse(json: Json): Payload = {
    val guildIdPath = root.d.guild_id.string
    val threadIdPath = root.d.id.string

    (for {
      guildId <- guildIdPath.getOption(json)
      sourceThreadId <- threadIdPath.getOption(json)
    } yield {
      Payload(guildId, sourceThreadId)
    }).getOrElse(throw new Exception("unexpected json"))
  }
}
