package discord

sealed abstract class BotContext() {}

object BotContext {
  case class Channel(name: String, id: String)

  case class InitializedBotContext(config: DiscordConfig) extends BotContext

  case class ReadyBotContext(config: DiscordConfig, times: List[Channel], meUserId: String) extends BotContext
}
