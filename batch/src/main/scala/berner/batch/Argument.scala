package berner.batch

case class Argument(
    command: String = ""
)

object Argument {
  object Command {
    final val ExchangeRate = "exchange-rate"
  }
}
