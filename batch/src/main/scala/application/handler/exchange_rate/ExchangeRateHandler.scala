package application.handler.exchange_rate

import application.lib.http.HttpClient
import cats.effect._
import io.circe.Json
import org.http4s.{Method, Request, Uri}

object ExchangeRateHandler {

  def handle(appId: String): IO[Unit] = {
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

    // https://docs.openexchangerates.org/reference/api-introduction
    val endpoint = "https://openexchangerates.org/api/latest.json"
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(endpoint).withQueryParam("app_id", appId)
    )

    for {
      httpClient <- HttpClient.make[IO]
      _ <- httpClient.expect[Json](request)
    } yield ()

  }

}
