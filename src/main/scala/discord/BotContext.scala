package discord

sealed abstract class BotContext()

object BotContext {
  case class Uninitialized() extends BotContext

  case class InitializedBotContext(userId: String) extends BotContext
}
