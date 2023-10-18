package application.handler.exchange_rate

import cats.effect._
import cats.syntax.all._
import io.circe.Json
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.{Headers, Method, Request, Uri, UrlForm}
import org.http4s.jdkhttpclient.JdkHttpClient

object ExchangeRateHandler {

  def handle[F[_]: Async](appId: String) = {
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

    val endpoint = "https://openexchangerates.org/api/latest.json"
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(endpoint).withQueryParam("app_id", appId)
    )
    for {
      httpClient <- JdkHttpClient.simple[F]
      httpClientWithLogger = RequestLogger(logHeaders = true, logBody = true)(ResponseLogger(logHeaders = true, logBody = true)(httpClient))
      response <- httpClientWithLogger.expect[Json](request)
    } yield {
      ???
    }

  }

}
