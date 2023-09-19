import cats.effect._
import database.Database
import database.service.HubMessageService
import discord.payload.{GuildCreate, Payload}
import discord.payload.Payload.DiscordEvent
import discord.{BotContext, DiscordConfig, GatewayClient}
import io.circe.Json
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import io.circe.generic.auto._, io.circe.syntax._
import application.lib.CirceConfig.config
import scala.concurrent.duration._

object Main extends IOApp {
  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory.getLogger

    Database.apply.use { transactor =>
      DiscordConfig.apply.use { config =>
        val hubMessageService = new HubMessageService(transactor)
        retry(
          {
            val jobHandler: (BotContext, Payload[Json]) => IO[BotContext] = { case (context, payload) =>
              payload.t
                .flatMap(DiscordEvent.fromString)
                .map {
                  case DiscordEvent.Ready =>
                    // todo 自身のIDを取得
                    ???
                  case DiscordEvent.GuildCreate =>
                    // todo 監視するチャンネルを絞り込む
                    payload.d.flatMap(_.as[GuildCreate.Data].toOption) match {
                      case Some(data) => ???
                      case None => ???
                    }
                  case DiscordEvent.MessageCreate => ???
                  case DiscordEvent.MessageDelete => ???
                  case DiscordEvent.MessageUpdate => ???
                  case DiscordEvent.ThreadCreate => ???
                  case DiscordEvent.ThreadDelete => ???
                  case _ => IO.pure(context)
                }
                .getOrElse(IO.pure(context))
            }
            GatewayClient.run(config, jobHandler)
          },
          100
        ).as(ExitCode.Success)
      }
    }
  }

  private def retry[A](ioa: IO[A], maxRetries: Int): IO[A] = {
    val logger = LoggerFactory.getLogger

    ioa.handleErrorWith { error =>
      if (maxRetries > 0) {
        logger.error(error)(s"failed to connect (maxRetries=$maxRetries)") *>
          IO.sleep(5.seconds) *> retry(ioa, maxRetries - 1)
      } else {
        IO.raiseError(error)
      }
    }
  }
}
