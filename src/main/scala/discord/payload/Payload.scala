package discord.payload

case class Payload[T](op: Int, d: Option[T], s: Option[Int], t: Option[String])
