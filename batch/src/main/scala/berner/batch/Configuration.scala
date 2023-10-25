package berner.batch

import cats.effect.IO
import com.typesafe.config.ConfigFactory

case class Configuration(
    openExchangeRatesAppId: String,
    discordBotTimesWebhookId: String,
    discordBotTimesWebhookToken: String
)

object Configuration {
  def load: IO[Configuration] = {
    for {
      config <- IO(ConfigFactory.load())
    } yield {
      Configuration(
        openExchangeRatesAppId = config.getString("openExchangeRates.appId"),
        discordBotTimesWebhookId = config.getString("discord.bot.times.webhook.id"),
        discordBotTimesWebhookToken = config.getString("discord.bot.times.webhook.token")
      )
    }
  }
}
