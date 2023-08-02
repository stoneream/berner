package discord

import cats.effect._
import cats.syntax.all._
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.{Method, Request, Uri, UrlForm}
import org.http4s.jdkhttpclient.JdkHttpClient

object DiscordWebhookClient {
  private def makeHttpClient[F[_]: Async](): F[Client[F]] = {
    for {
      httpClient <- JdkHttpClient.simple[F]
      httpClientWithLogger = RequestLogger(logHeaders = true, logBody = true)(ResponseLogger(logHeaders = true, logBody = true)(httpClient))
    } yield httpClientWithLogger
  }

  /**
   * https://discord.com/developers/docs/resources/webhook#execute-webhook
   */
  def execute(content: String, username: String, avatarUrl: String)(webhookId: String, webhookToken: String): IO[Json] = {
    for {
      httpClient <- makeHttpClient[IO]()
      request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"https://discord.com/api/webhooks/$webhookId/$webhookToken").withQueryParam("wait", "true")
      ).withEntity {
        UrlForm(
          ("content", content),
          ("username", username),
          ("avatar_url", avatarUrl)
        )
      }
      response <- httpClient.expect[Json](request)
    } yield response
  }
}
