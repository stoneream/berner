package berner.core.model

import java.time.LocalDateTime

case class ExchangeRate(
    id: Long,
    baseCurrency: String,
    targetCurrency: String,
    rate: BigDecimal,
    targetDate: LocalDateTime,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: Option[LocalDateTime]
)
