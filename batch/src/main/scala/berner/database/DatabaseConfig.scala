package berner.database

import com.typesafe.config.Config

case class DatabaseConfig(
    driver: String,
    url: String,
    user: String,
    password: String,
    poolMaxSize: Int
)

object DatabaseConfig {
  def fromConfig(config: Config): DatabaseConfig = {
    DatabaseConfig(
      driver = config.getString("database.primary.driver"),
      url = config.getString("database.primary.url"),
      user = config.getString("database.primary.user"),
      password = config.getString("database.primary.password"),
      poolMaxSize = config.getInt("database.primary.poolMaxSize")
    )
  }
}
