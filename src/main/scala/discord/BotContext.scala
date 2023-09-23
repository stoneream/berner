package discord

sealed abstract class BotContext() {}

object BotContext {
  case class Init(config: DiscordConfig) extends BotContext

  case class Ready(
      config: DiscordConfig,
      me: payload.Ready.User
  ) extends BotContext
}
