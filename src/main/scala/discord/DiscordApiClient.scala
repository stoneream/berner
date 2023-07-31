package discord

import cats.effect._
import cats.syntax.all._
import io.circe.Json
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.client.middleware.RequestLogger
import org.http4s.{Headers, Method, Request, Uri, UrlForm}
import org.http4s.jdkhttpclient.JdkHttpClient

// ref : https://discord.com/developers/docs/reference
object DiscordApiClient {

  private def makeHttpClient[F[_]: Async](): F[Client[F]] = {
    for {
      httpClient <- JdkHttpClient.simple[F]
      httpClientWithLogger = RequestLogger(logHeaders = true, logBody = true)(httpClient)
    } yield httpClientWithLogger
  }

  /**
   * https://discord.com/developers/docs/resources/channel#create-message
   */
  def createMessage(content: String, channelId: String)(token: String): IO[Json] = {
    for {
      httpClient <- makeHttpClient[IO]()
      request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"https://discord.com/api/v10/channels/$channelId/messages"),
        headers = Headers(
          "Authorization" -> s"Bot ${token}"
        )
      ).withEntity {
        UrlForm(("content", content))
      }
      response <- httpClient.expect[Json](request)
    } yield response

  }
}
