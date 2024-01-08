package berner.bot.v2.model.hub

sealed abstract class HubContext() {}

object HubContext {
  case class Uninitialized() extends HubContext()

  case class Initialized() extends HubContext()
}
