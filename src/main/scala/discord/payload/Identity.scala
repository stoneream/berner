package discord.payload

case class Identity(token: String, intents: Int, properties: Map[String, String])
