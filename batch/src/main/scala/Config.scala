case class Config(
    command: String = ""
)

object Config {
  object Command {
    final val ExchangeRate = "exchange-rate"
  }
}
