package berner.batch.handler.exchange_rate

import berner.core.model.ExchangeRate
import berner.core.open_exchange_rates.OpenExchangeRatesClient
import berner.core.open_exchange_rates.payload.{AppId, Latest}
import cats.data.ReaderT
import cats.implicits._
import cats.effect._
import doobie.util.transactor.Transactor

import java.time.{Instant, LocalDateTime, ZoneOffset}

object ExchangeRateHandler {

  def handle(appId: String): ReaderT[IO, Transactor[IO], Unit] = ReaderT { transactor =>
    for {
      appId <- IO.pure(AppId(appId))
      response <- OpenExchangeRatesClient.latest()(appId)
      exchangeRates = convert(response)
      _ <- write(exchangeRates).run(transactor)
    } yield ()
  }

  private def convert(latest: Latest): List[ExchangeRate] = {
    val now = LocalDateTime.now
    val instant = Instant.ofEpochSecond(latest.timestamp)
    val targetDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    latest.rates.map { case (currency, rate) =>
      ExchangeRate(
        id = 0L,
        baseCurrency = latest.base,
        targetCurrency = currency,
        rate = rate.setScale(4, BigDecimal.RoundingMode.DOWN),
        targetDate = targetDate,
        createdAt = now,
        updatedAt = now,
        deletedAt = None
      )
    }.toList
  }

  private def write(ers: List[ExchangeRate]): ReaderT[IO, Transactor[IO], Int] = ReaderT { transactor =>
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

    type InsertExchangeRate = (String, String, BigDecimal, LocalDateTime, LocalDateTime, LocalDateTime, Option[LocalDateTime])

    val rows = ers.map { er =>
      (er.baseCurrency, er.targetCurrency, er.rate, er.targetDate, er.createdAt, er.updatedAt, er.deletedAt)
    }

    Update[InsertExchangeRate](query).updateMany(rows).transact(transactor)
  }

}
