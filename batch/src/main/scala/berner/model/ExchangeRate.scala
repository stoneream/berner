package berner.model

import java.time.OffsetDateTime

case class ExchangeRate(
    id: Long,
    baseCurrency: String,
    targetCurrency: String,
    rate: BigDecimal,
    targetDate: OffsetDateTime,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)
