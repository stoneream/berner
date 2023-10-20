package discord

import com.typesafe.config.Config

case class DiscordConfig(
    token: String,
    timesHubWebhookId: String,
    timesHubWebhookToken: String
)

object DiscordConfig {
  def fromConfig(config: Config): DiscordConfig = {
    DiscordConfig(
      token = config.getString("discord.token"),
      timesHubWebhookId = config.getString("discord.times.hub.webhook.id"),
      timesHubWebhookToken = config.getString("discord.times.hub.webhook.token")
    )
  }
}
