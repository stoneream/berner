package discord

sealed abstract class BotContext() {}

object BotContext {
  case class Channel(name: String, id: String)

  case class InitializedBotContext(token: String) extends BotContext

  case class ReadyBotContext(token: String, times: List[Channel], hubTimes: Option[Channel], meUserId: String) extends BotContext
}
