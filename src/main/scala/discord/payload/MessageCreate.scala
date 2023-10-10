package discord.payload

object MessageCreate {
  case class Data(
      id: String,
      guildId: String,
      channelId: String,
      author: Author,
      content: String,
      mentions: Seq[Mention],
      attachments: Seq[Attachment]
  )

  case class Author(
      id: String,
      username: String,
      avatar: Option[String]
  )

  case class Mention(
      globalName: String,
      id: String
  )

  case class Attachment(
      url: String
  )
}
