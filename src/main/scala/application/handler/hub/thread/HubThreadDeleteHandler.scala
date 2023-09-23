package application.handler.hub.thread

import application.ApplicationContext
import cats.data.ReaderT
import cats.effect.IO
import cats.implicits.toTraverseOps
import database.service.HubMessageService
import discord.payload.ThreadDelete
import discord.{BotContext, DiscordApiClient}
import io.circe.Json
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

import java.time.LocalDateTime

object HubThreadDeleteHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json)(hubMessageService: HubMessageService[IO]): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.evalGetAndUpdate { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          json
            .as[ThreadDelete.Data]
            .toOption
            .map { d =>
              val hubContext = context.hubContext
              val botConfig = botContext.config
              hubMessageService
                .findBySourceChannelId(d.id, d.guildId)
                .flatMap { hubMessages =>
                  hubMessages.traverse { hubMessage =>
                    for {
                      _ <- DiscordApiClient.deleteMessage(hubMessage.channelId, hubMessage.messageId)(botConfig.token)
                      _ <- hubMessageService.delete(hubMessage.id, LocalDateTime.now())
                    } yield ()
                  }
                }
                .map { _ =>
                  val nextTimesThreads = hubContext.timesThreads.removed(d.id)
                  val nextHubContext = hubContext.copy(timesThreads = nextTimesThreads)
                  val nextApplicationContext = context.copy(hubContext = nextHubContext)
                  nextApplicationContext
                }
            }
            .getOrElse(IO.raiseError(new RuntimeException("Unexpected json")))
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }.void
  }
}
