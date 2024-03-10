package berner.database.writer

import berner.model.ExchangeRate
import cats.data.ReaderT
import cats.implicits._
import cats.effect._
import doobie.util.transactor.Transactor

import java.time.OffsetDateTime

object ExchangeRateWriter {

  def write(ls: List[ExchangeRate]): ReaderT[IO, Transactor[IO], Int] = ReaderT { transactor =>
    import doobie._
    import doobie.implicits._
    import doobie.implicits.javatimedrivernative._

    val query =
      """
        |INSERT INTO exchange_rates (
        |base_currency,
        |target_currency,
        |rate,
        |target_date,
        |created_at,
        |updated_at,
        |deleted_at
        |) VALUES (?, ?, ?, ?, ?, ?, ?)
        |""".stripMargin

    type InsertExchangeRate = (
        String,
        String,
        BigDecimal,
        OffsetDateTime,
        OffsetDateTime,
        OffsetDateTime,
        Option[OffsetDateTime]
    )

    val rows = ls.map { er =>
      (
        er.baseCurrency,
        er.targetCurrency,
        er.rate,
        er.targetDate,
        er.createdAt,
        er.updatedAt,
        er.deletedAt
      )
    }

    Update[InsertExchangeRate](query).updateMany(rows).transact(transactor)
  }

}
