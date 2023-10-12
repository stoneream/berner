package discord.payload

object ThreadCreate {
  case class Data(name: String, id: String, parentId: String)
}
