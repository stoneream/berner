package berner.batch

import berner.batch.Argument.Command
import berner.batch.handler.exchange_rate.ExchangeRateHandler
import berner.core.database.{Database, DatabaseConfig}
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import doobie.util.ExecutionContexts
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import scopt.OParser

object Main extends IOApp {
  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory.getLogger

    for {
      arguments <- parseArgs(args)
      config <- Configuration.load
      _ <- logger.info(s"[arguments=$arguments]")
      _ <- runCommand(arguments, config)
    } yield ExitCode.Success
  }

  private def parseArgs(args: List[String]): IO[Argument] = {
    val parser = {
      val builder = OParser.builder[Argument]
      import builder._
      OParser.sequence(
        head("batch", "0.1.0"),
        cmd(Command.ExchangeRate)
          .action((_, c) => c.copy(command = Command.ExchangeRate))
      )
    }

    OParser.parse(parser, args, Argument()) match {
      case Some(config) => IO.pure(config)
      case None => IO.raiseError(new IllegalArgumentException("invalid arguments"))
    }
  }

  private def runCommand(arguments: Argument, config: Configuration): IO[Unit] = {
    for {
      databaseConfig <- IO(ConfigFactory.load()).map(DatabaseConfig.fromConfig)
      _ <- ExecutionContexts.fixedThreadPool[IO](databaseConfig.poolMaxSize).use { ec =>
        Database.apply(databaseConfig, ec).use { transactor =>
          arguments.command match {
            case Command.ExchangeRate => ExchangeRateHandler.handle(config.openExchangeRatesAppId)(transactor)
          }
        }
      }
    } yield ()
  }
}
