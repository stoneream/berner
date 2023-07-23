import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory

case class Config(
    discordToken: String
)

object Config {
  def apply: Resource[IO, Config] = {
    Resource.eval(IO(ConfigFactory.load())).map { config =>
      Config(
        discordToken = config.getString("discord.token")
      )
    }
  }
}
