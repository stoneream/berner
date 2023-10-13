import cats.effect.{ExitCode, IO, IOApp}
import scopt.OParser
import Config.Command
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp {
  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory.getLogger

    for {
      config <- parseArgs(args)
      _ <- logger.info(s"[$config]")
    } yield ExitCode.Success
  }

  private def parseArgs(args: List[String]): IO[Config] = {
    val parser = {
      val builder = OParser.builder[Config]
      import builder._
      OParser.sequence(
        head("batch", "0.1.0"),
        cmd(Command.ExchangeRate)
          .action((_, c) => c.copy(command = Command.ExchangeRate))
      )
    }

    OParser.parse(parser, args, Config()) match {
      case Some(config) => IO.pure(config)
      case None => IO.raiseError(new IllegalArgumentException("invalid arguments"))
    }
  }
}
