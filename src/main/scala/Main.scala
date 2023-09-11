import cats.effect._
import database.Database
import database.service.HubMessageService
import discord.handler.{GuildCreateHandler, MessageCreateHandler, MessageDeleteHandler, MessageUpdateHandler, ReadyHandler, ThreadCreateHandler, ThreadDeleteHandler}
import discord.payload.Payload.DiscordEvent
import discord.{BotContext, DiscordConfig, GatewayClient}
import io.circe.Json
import io.circe.optics.JsonPath._
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory

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
            val jobHandler: (BotContext, Json) => IO[BotContext] = { case (context, json) =>
              root.t.string.getOption(json) match {
                case Some(value) =>
                  DiscordEvent.fromString(value) match {
                    case Some(DiscordEvent.Ready) => ReadyHandler.handle(json)(context)
                    case Some(DiscordEvent.MessageCreate) => MessageCreateHandler.handle(json)(context, hubMessageService)
                    case Some(DiscordEvent.GuildCreate) => GuildCreateHandler.handle(json)(context)
                    case Some(DiscordEvent.MessageDelete) => MessageDeleteHandler.handle(json)(context, hubMessageService)
                    case Some(DiscordEvent.MessageUpdate) => MessageUpdateHandler.handle(json)(context, hubMessageService)
                    case Some(DiscordEvent.ThreadCreate) => ThreadCreateHandler.handle(json)(context)
                    case Some(DiscordEvent.ThreadDelete) => ThreadDeleteHandler.handle(json)(context, hubMessageService)
                    case Some(_) => IO.pure(context)
                    case None => logger.warn("unknown event").as(context)
                  }
                case None => IO.pure(context)
              }
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
