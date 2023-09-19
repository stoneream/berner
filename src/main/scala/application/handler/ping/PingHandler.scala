package application.handler.ping

import application.ApplicationContext
import cats.effect.IO
import discord.{BotContext, DiscordApiClient}

object PingHandler {
  def handle(sourceChannelId: String)(context: ApplicationContext): IO[ApplicationContext] = {
    context.discordBotContext match {
      case BotContext.Init(_) => ???
      case BotContext.Ready(config, _, _, _) =>
        for {
          _ <- DiscordApiClient.createMessage("pong", sourceChannelId)(config.token)
        } yield {
          context
        }
    }
  }
}
