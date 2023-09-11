package discord

sealed abstract class BotContext() {}

object BotContext {
  case class Channel(name: String, id: String)

  case class Thread(name: String, id: String, parentId: String)

  case class InitializedBotContext(config: DiscordConfig) extends BotContext

  case class ReadyBotContext(
      config: DiscordConfig,
      times: List[Channel], // todo idで引けるようになってたほうが嬉しい気がする
      timesThreads: List[Thread], // todo idで引けるようになってたほうが嬉しい気がする
      meUserId: String
  ) extends BotContext
}
