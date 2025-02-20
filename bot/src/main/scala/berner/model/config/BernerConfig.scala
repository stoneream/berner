package berner.model.config

case class BernerConfig(
    discord: BernerConfig.Discord
)

object BernerConfig {
  case class Discord(
      token: String
  )

  def load(): BernerConfig = {
    val config = com.typesafe.config.ConfigFactory.load()

    BernerConfig(
      discord = Discord(
        token = config.getString("discord.token")
      )
    )
  }
}
