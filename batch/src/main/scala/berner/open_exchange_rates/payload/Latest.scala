package berner.open_exchange_rates.payload

case class Latest(
    disclaimer: String,
    license: String,
    base: String,
    timestamp: Long,
    rates: Map[String, BigDecimal]
)
