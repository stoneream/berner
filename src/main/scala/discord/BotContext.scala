package discord

sealed abstract class BotContext() {}

object BotContext {
  case class Channel(name: String, id: String)

  case class Thread(name: String, id: String, parentId: String)

  case class Init(config: DiscordConfig) extends BotContext

  case class Ready(
      config: DiscordConfig,
      @deprecated("そのうち消す")
      times: List[Channel], // todo idで引けるようになってたほうが嬉しい気がする
      @deprecated("そのうち消す")
      timesThreads: List[Thread], // todo idで引けるようになってたほうが嬉しい気がする
      meUserId: String, // todo userオブジェクト持ってる方が良さそう
      me: payload.Ready.User
  ) extends BotContext
}
