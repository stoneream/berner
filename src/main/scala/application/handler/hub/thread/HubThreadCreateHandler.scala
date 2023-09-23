package application.handler.hub.thread

import application.ApplicationContext
import application.handler.hub.HubContext
import cats.data.ReaderT
import cats.effect.IO
import discord.BotContext
import discord.payload.ThreadCreate
import io.circe.Json
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

object HubThreadCreateHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.evalGetAndUpdate { currentContext =>
      currentContext.discordBotContext match {
        case _: BotContext.Ready =>
          json
            .as[ThreadCreate.Data]
            .toOption
            .map { d =>
              val hubContext = currentContext.hubContext
              if (hubContext.timesChannels.contains(d.parentId)) {
                val nextTimesThreads = hubContext.timesThreads.updated(d.id, HubContext.Thread(d.name, d.id, d.parentId))
                val nextHubContext = hubContext.copy(timesThreads = nextTimesThreads)
                val nextApplicationContext = currentContext.copy(hubContext = nextHubContext)
                IO(nextApplicationContext)
              } else {
                IO(currentContext)
              }
            }
            .getOrElse(IO.raiseError(new RuntimeException("Unexpected Json")))
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }.void
  }
}
