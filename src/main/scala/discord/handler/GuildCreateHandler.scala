package discord.handler

import cats.effect.IO
import discord.BotContext
import discord.BotContext.Channel
import io.circe.Json
import io.circe.optics.JsonPath.root

object GuildCreateHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {
    context match {
      case BotContext.InitializedBotContext(_) => IO.raiseError(new Exception("unexpected bot context"))
      case BotContext.ReadyBotContext(token, _, hubTimesOpt, meUserId) =>
        // ウォッチ対象のサーバーとして登録
        val channelsPath = root.d.channels.each.json
        val channelNamePath = root.name.string
        val channelIdPath = root.id.string

        val channels = channelsPath.getAll(json).flatMap { channel =>
          for {
            channelId <- channelIdPath.getOption(channel)
            channelName <- channelNamePath.getOption(channel)
          } yield {
            Channel(channelName, channelId)
          }
        }

        val times = channels.filter(_.name.startsWith("times-"))
        val hubTimes = channels.find(_.name == "hub-times")

        val nextContext = BotContext.ReadyBotContext(
          token = token,
          times = times,
          hubTimes = hubTimes,
          meUserId = meUserId
        )

        IO.pure(nextContext)
    }
  }
}
