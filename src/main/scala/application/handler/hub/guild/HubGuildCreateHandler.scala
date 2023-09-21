package application.handler.hub.guild

import application.ApplicationContext
import application.handler.hub.HubContext
import application.handler.hub.HubContext.Channel
import cats.data.ReaderT
import cats.effect.IO
import discord.BotContext
import discord.payload.GuildCreate
import io.circe.Json
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.auto._

object HubGuildCreateHandler {
  private implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

  def handle(json: Json): ApplicationContext.Handler[Unit] = ReaderT { state =>
    state.evalGetAndUpdate { context =>
      context.discordBotContext match {
        case botContext: BotContext.Ready =>
          json.as[GuildCreate.Data].toOption match {
            case Some(d) =>
              // timesで始まるチャンネルを監視対象として登録
              val timesChannels = d.channels
                .filter(_.name.startsWith("times-"))
                .map { c =>
                  (c.id, HubContext.Channel(c.name, c.id))
                }
                .toMap
              // times内のスレッドも監視対象とする
              val timesThreads = d.threads
                .filter(t => timesChannels.contains(t.parentId))
                .map { t =>
                  (t.id, HubContext.Thread(t.name, t.id))
                }
                .toMap

              // 今のところこれで良い
              val nextHubContext = context.hubContext.copy(
                timesChannels = timesChannels,
                timesThreads = timesThreads
              )

              val nextContext = context.copy(hubContext = nextHubContext)

              IO(nextContext)
            case _ => IO.raiseError(new RuntimeException("Unexpected Json"))
          }
        case _ => IO.raiseError(new RuntimeException("Unexpected BotContext"))
      }
    }
  }
}
