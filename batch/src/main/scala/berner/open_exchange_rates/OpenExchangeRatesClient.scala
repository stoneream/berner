package berner.open_exchange_rates

import berner.http.HttpClient
import berner.open_exchange_rates.payload.{AppId, Latest}
import cats.data.ReaderT
import cats.effect.IO
import io.circe.Json
import org.http4s.{Method, Request, Uri}
import io.circe.generic.auto._

object OpenExchangeRatesClient {

  def latest(baseCurrency: String = "USD"): ReaderT[IO, AppId, Latest] = ReaderT { appId =>
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

    // https://docs.openexchangerates.org/reference/api-introduction
    val endpoint = "https://openexchangerates.org/api/latest.json"
    val request = Request[IO](
      method = Method.POST,
      uri = Uri
        .unsafeFromString(endpoint)
        .withQueryParam("app_id", appId.value)
        .withQueryParam("base", baseCurrency)
    )

    for {
      httpClient <- HttpClient.make[IO]
      json <- httpClient.expect[Json](request)
      result <- json.as[Latest].fold(IO.raiseError(_), IO.pure)
    } yield result
  }

}
