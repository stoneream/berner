package discord

import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class DiscordConfig(
    token: String
)

object DiscordConfig {
  def apply: Resource[IO, DiscordConfig] = {
    Resource.eval(IO(ConfigFactory.load())).map { config =>
      DiscordConfig(
        token = config.getString("discord.token")
      )
    }
  }
}
