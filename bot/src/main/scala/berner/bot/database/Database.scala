package berner.bot.database

import cats.effect._
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

object Database {
  def apply(
      config: DatabaseConfig,
      ec: ExecutionContext
  ): Resource[IO, HikariTransactor[IO]] = {
    HikariTransactor.newHikariTransactor[IO](
      config.driver,
      config.url,
      config.user,
      config.password,
      ec
    )
  }

  def apply: Resource[IO, HikariTransactor[IO]] = {
    for {
      config <- DatabaseConfig.default
      ec <- ExecutionContexts.fixedThreadPool[IO](config.poolMaxSize)
      transactor <- Database.apply(config, ec)
    } yield transactor
  }
}
