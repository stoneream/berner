package berner.core.discord

import berner.core.http.HttpClient
import cats.effect._
import io.circe.Json
import org.http4s.{Headers, Method, Request, Uri, UrlForm}

// ref : https://discord.com/developers/docs/reference
object DiscordApiClient {

  /**
   * https://discord.com/developers/docs/resources/channel#create-message
   */
  def createMessage(content: String, channelId: String)(token: String): IO[Json] = {
    import org.http4s.circe.CirceEntityCodec.circeEntityDecoder

    for {
      httpClient <- HttpClient.make[IO]
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
      httpClient <- HttpClient.make[IO]
      request = Request[IO](
        method = Method.DELETE,
        uri = Uri.unsafeFromString(s"https://discord.com/api/v10/channels/${channelId}/messages/${messageId}"),
        headers = Headers(
          "Authorization" -> s"Bot ${token}"
        )
      )
      _ <- httpClient.expect[String](request)
    } yield ()
  }
}
