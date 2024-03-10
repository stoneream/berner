package berner.discord

import berner.http.HttpClient
import cats.effect._
import io.circe.Json
import org.http4s.circe.jsonDecoder
import org.http4s.{Method, Request, Uri, UrlForm}

object DiscordWebhookClient {

  /**
   * https://discord.com/developers/docs/resources/webhook#execute-webhook
   */
  def execute(content: String, username: String, avatarUrl: Option[String])(webhookId: String, webhookToken: String): IO[Json] = {
    for {
      httpClient <- HttpClient.make[IO]
      request = Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(s"https://discord.com/api/webhooks/$webhookId/$webhookToken").withQueryParam("wait", "true")
      ).withEntity {
        avatarUrl.fold {
          UrlForm(
            ("content", content),
            ("username", username)
          )
        } { avatarUrl =>
          UrlForm(
            ("content", content),
            ("username", username),
            ("avatar_url", avatarUrl)
          )
        }
      }
      response <- httpClient.expect[Json](request)
    } yield response
  }
}
