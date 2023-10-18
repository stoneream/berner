package berner.batch

import cats.effect.IO
import com.typesafe.config.ConfigFactory

case class Configuration(
    openExchangeRatesAppId: String
)

object Configuration {
  def load: IO[Configuration] = {
    for {
      config <- IO(ConfigFactory.load())
    } yield {
      Configuration(
        openExchangeRatesAppId = config.getString("openExchangeRates.appId")
      )
    }
  }
}
