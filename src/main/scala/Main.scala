import application.Config
import cats.effect._
import discord.GatewayClient
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.concurrent.duration._

object Main extends IOApp {

  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Config.apply.use { config =>
      reconnect(0, config).as(ExitCode.Success)
    }
  }

  // todo Bot起動前にデータベースを初期化する
  // todo 処理を関数に切り出す
  private def reconnect(attempt: Int, config: Config): IO[Unit] = {
    val logger = LoggerFactory.getLogger

    if (attempt > 5) {
      IO.raiseError(new Exception("failed to connect"))
    } else {
      GatewayClient.run(config).handleErrorWith { err =>
        for {
          _ <- logger.error(err)(s"failed to connect (attempt=$attempt)")
          _ <- IO.sleep(5.seconds)
          _ <- reconnect(attempt + 1, config)
        } yield {}
      }
    }
  }
}
