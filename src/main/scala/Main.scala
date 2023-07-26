import application.Config
import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import discord.{GatewayClient, GatewayOpCode}
import fs2.Stream
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.net.http.HttpClient
import scala.concurrent.duration.*

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
      IO.raiseError(Exception("failed to connect"))
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
