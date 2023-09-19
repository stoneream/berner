package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath.root

object GuildCreateHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {
    context match {
      case BotContext.Init(_) => IO.raiseError(new Exception("unexpected bot context"))
      case BotContext.Ready(config, _, _, meUserId) =>
        // ウォッチ対象のサーバーとして登録
        val channelsPath = root.d.channels.each.json
        val channelNamePath = root.name.string
        val channelIdPath = root.id.string
        val threadsPath = root.d.threads.each.json

        val channels = channelsPath.getAll(json).flatMap { channel =>
          for {
            channelId <- channelIdPath.getOption(channel)
            channelName <- channelNamePath.getOption(channel)
          } yield {
            BotContext.Channel(channelName, channelId)
          }
        }

        val times = channels.filter(_.name.startsWith("times-"))

        val threads = threadsPath.getAll(json).flatMap { thread =>
          for {
            parentId <- root.parent_id.string.getOption(thread)
            threadId <- root.id.string.getOption(thread)
            name <- root.name.string.getOption(thread)
          } yield {
            BotContext.Thread(name, threadId, parentId)
          }
        }

        val timesThreads = threads.filter(thread => times.exists(_.id == thread.parentId))

        val nextContext = BotContext.Ready(
          config = config,
          times = times,
          timesThreads = timesThreads,
          meUserId = meUserId
        )

        IO.pure(nextContext)
    }
  }
}
