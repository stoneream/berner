package berner.core.database

import cats.effect.IO
import cats.effect.kernel.Resource
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

object Database {
  def apply(config: DatabaseConfig, ec: ExecutionContext): Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
      ec
    )
  }

}
