package database

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
        driver = config.getString("database.default.driver"),
        url = config.getString("database.default.url"),
        user = config.getString("database.default.user"),
        password = config.getString("database.default.password"),
        poolMaxSize = config.getInt("database.default.poolMaxSize")
      )
    }
  }
}
