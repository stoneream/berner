package berner.bot.database

import cats.effect._
import com.typesafe.config.ConfigFactory

case class DatabaseConfig(
    driver: String,
    url: String,
    user: String,
    password: String,
    poolMaxSize: Int
) {}

object DatabaseConfig {
  def default: Resource[IO, DatabaseConfig] = {
    Resource.eval(IO(ConfigFactory.load())).map { config =>
      DatabaseConfig(
        driver = config.getString("database.primary.driver"),
        url = config.getString("database.primary.url"),
        user = config.getString("database.primary.user"),
        password = config.getString("database.primary.password"),
        poolMaxSize = config.getInt("database.primary.poolMaxSize")
      )
    }
  }
}
