package berner.batch.handler.exchange_rate

import berner.batch.Configuration
import berner.core.database.writer.ExchangeRateWriter
import berner.core.discord.DiscordWebhookClient
import berner.core.model.ExchangeRate
import berner.core.open_exchange_rates.OpenExchangeRatesClient
import berner.core.open_exchange_rates.payload.{AppId, Latest}
import cats.data.ReaderT
import cats.effect._
import doobie.util.transactor.Transactor

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}

object ExchangeRateHandler {

  def handle(): ReaderT[IO, (Configuration, Transactor[IO]), Unit] = ReaderT { case (configuration, transactor) =>
    for {
      appId <- IO.pure(AppId(configuration.openExchangeRatesAppId))
      response <- OpenExchangeRatesClient.latest()(appId)
      exchangeRates = convert(response)
      _ <- ExchangeRateWriter.write(exchangeRates).run(transactor)
      message = makeMessage(response)
      _ <- DiscordWebhookClient.execute(message, "為替レート", None)(configuration.discordBotTimesWebhookId, configuration.discordBotTimesWebhookToken).void
    } yield ()
  }

  private def convert(latest: Latest): List[ExchangeRate] = {
    val now = OffsetDateTime.now
    val instant = Instant.ofEpochSecond(latest.timestamp)
    val targetDate = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC)

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

  private def makeMessage(latest: Latest): String = {
    val rates = latest.rates
    val instant = Instant.ofEpochSecond(latest.timestamp)
    val targetDate = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).atOffset(ZoneOffset.UTC)
    val formattedDate = targetDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    (for {
      jpy <- rates.get("JPY")
    } yield {
      s"""USD/JPY ${jpy}
         |
         |($formattedDate)
         |""".stripMargin
    }).getOrElse("為替レートの取得に失敗")
  }

}
