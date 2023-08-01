package discord

sealed abstract class BotContext() {}

object BotContext {
  case class InitializedBotContext(token: String) extends BotContext

  case class ReadyBotContext(token: String, userId: String) extends BotContext
}
