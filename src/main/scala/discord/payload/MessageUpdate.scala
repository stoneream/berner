package discord.payload

object MessageUpdate {
  case class Data(
      guildId: String,
      id: String,
      channelId: String,
      content: String,
      mentions: Seq[Mention]
  )

  case class Mention(
      globalName: String,
      id: String
  )
}
