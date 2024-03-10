package berner.batch

import cats.effect.IO
import com.typesafe.config.ConfigFactory

case class Configuration(
    openExchangeRatesAppId: String,
    discordBotWebhookId: String,
    discordBotWebhookToken: String
)

object Configuration {
  def load: IO[Configuration] = {
    for {
      config <- IO(ConfigFactory.load())
    } yield {
      Configuration(
        openExchangeRatesAppId = config.getString("openExchangeRates.appId"),
        discordBotWebhookId = config.getString("discord.bot.webhook.id"),
        discordBotWebhookToken = config.getString("discord.bot.webhook.token")
      )
    }
  }
}
