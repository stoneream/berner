package discord.payload

object MessageDelete {
  case class Data(
      guildId: String,
      id: String,
      channelId: String
  )
}
