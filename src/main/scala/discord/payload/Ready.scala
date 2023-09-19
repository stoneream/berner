package discord.payload

object Ready {
  case class Data(
      user: User
  )

  case class User(
      id: String
  )
}
