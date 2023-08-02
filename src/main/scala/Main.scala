import cats.effect._
import database.Database
import discord.handler.{GuildCreateHandler, MessageCreateHandler, ReadyHandler}
import discord.payload.Payload.DiscordEvent

import discord.{BotContext, DiscordConfig, GatewayClient}
import io.circe.Json
import io.circe.optics.JsonPath._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.concurrent.duration._

object Main extends IOApp {
  // ここらへんでtransactorを引き回す

  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Database.apply.use { transactor =>
      DiscordConfig.apply.use { config =>
        reconnect(0, config).as(ExitCode.Success)
      }
    }
  }

  // todo 処理を関数に切り出す
  private def reconnect(attempt: Int, config: DiscordConfig): IO[Unit] = {
    val logger = LoggerFactory.getLogger

    if (attempt > 5) {
      IO.raiseError(new Exception("failed to connect"))
    } else {
      val jobHandler: (BotContext, Json) => IO[BotContext] = { case (context, json) =>
        root.t.string.getOption(json) match {
          case Some(value) =>
            DiscordEvent.fromString(value) match {
              case Some(DiscordEvent.Ready) => ReadyHandler.handle(json)(context)
              case Some(DiscordEvent.MessageCreate) => MessageCreateHandler.handle(json)(context)
              case Some(DiscordEvent.GuildCreate) => GuildCreateHandler.handle(json)(context)
              // todo メッセージ消えたケース
              // todo メッセージ編集されたケース
              case Some(_) => IO.pure(context)
              case None => logger.warn("unknown event").as(context)
            }
          case None => logger.warn(json.noSpaces).as(context)
        }
      }

      GatewayClient.run(config, jobHandler).handleErrorWith { err =>
        for {
          _ <- logger.error(err)(s"failed to connect (attempt=$attempt)")
          _ <- IO.sleep(5.seconds)
          _ <- reconnect(attempt + 1, config)
        } yield {}
      }
    }
  }
}
