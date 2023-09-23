package discord.payload

object GuildCreate {
  case class Data(
      channels: List[Channel],
      threads: List[Thread]
  )

  case class Channel(
      id: String,
      name: String
  )

  case class Thread(
      id: String,
      parentId: String,
      name: String
  )

}
