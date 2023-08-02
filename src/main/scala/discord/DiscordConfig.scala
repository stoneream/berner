package discord

import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class DiscordConfig(
    token: String,
    timesHubWebhookId: String,
    timesHubWebhookToken: String
)

object DiscordConfig {
  def apply: Resource[IO, DiscordConfig] = {
    Resource.eval(IO(ConfigFactory.load())).map { config =>
      DiscordConfig(
        token = config.getString("discord.token"),
        timesHubWebhookId = config.getString("discord.times.hub.webhook.id"),
        timesHubWebhookToken = config.getString("discord.times.hub.webhook.token")
      )
    }
  }
}
