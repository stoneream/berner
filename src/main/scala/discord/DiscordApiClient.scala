package discord

import cats.effect._
import cats.syntax.all._
import io.circe.Json
import org.http4s.client.Client
import org.http4s.client.middleware.{RequestLogger, ResponseLogger}
import org.http4s.{Headers, Method, Request, Uri, UrlForm}
import org.http4s.jdkhttpclient.JdkHttpClient

// ref : https://discord.com/developers/docs/reference
object DiscordApiClient {

  private def makeHttpClient[F[_]: Async](): F[Client[F]] = {
    for {
      httpClient <- JdkHttpClient.simple[F]
      httpClientWithLogger = RequestLogger(logHeaders = true, logBody = true)(ResponseLogger(logHeaders = true, logBody = true)(httpClient))
    } yield httpClientWithLogger
  }

  /**
   * https://discord.com/developers/docs/resources/channel#create-message
   */
  def createMessage(content: String, channelId: String)(token: String): IO[Json] = {
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

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

  /**
   * https://discord.com/developers/docs/resources/channel#delete-message
   */
  def deleteMessage(channelId: String, messageId: String)(token: String): IO[Unit] = {
    for {
      httpClient <- makeHttpClient[IO]()
      request = Request[IO](
        method = Method.DELETE,
        uri = Uri.unsafeFromString(s"https://discord.com/api/v10/channels/${channelId}/messages/${messageId}"),
        headers = Headers(
          "Authorization" -> s"Bot ${token}"
        )
      )
      response <- httpClient.expect[String](request)
    } yield response
  }
}
