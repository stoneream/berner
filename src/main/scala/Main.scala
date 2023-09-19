import application.ApplicationContext
import application.handler.gateway.GatewayReadyHandler
import application.handler.hub.HubContext
import cats.effect._
import cats.effect.std.Queue
import cats.implicits.catsSyntaxTuple2Parallel
import database.Database
import database.service.HubMessageService
import discord.payload.Payload
import discord.payload.Payload.DiscordEvent
import discord.{BotContext, DiscordConfig, GatewayClient}
import fs2.Stream
import io.circe.Json
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

        val initialApplicationContext = ApplicationContext(
          discordBotContext = BotContext.Init(config),
          hubContext = HubContext.empty
        )

        Queue
          .unbounded[IO, Payload[Json]]
          .flatMap { messageQueue =>
            val applicationStream = Stream.eval(IO(initialApplicationContext)).flatMap { context =>
              Stream.fromQueueUnterminated(messageQueue).evalTap { payload =>
                (for {
                  d <- payload.d
                  t <- payload.t
                  nextContext <- DiscordEvent.fromString(t).collect { case DiscordEvent.Ready =>
                    GatewayReadyHandler.handle(d).run(context)
                  }
                } yield nextContext).getOrElse(IO(context, ()))
              }
            }

            for {
              _ <- (
                GatewayClient.run(messageQueue)(config),
                applicationStream.compile.drain
              ).parTupled
            } yield ()
          }
          .as(ExitCode.Success)
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
