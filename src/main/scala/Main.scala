import application.ApplicationContext
import application.handler.gateway.GatewayReadyHandler
import application.handler.hub.HubContext
import application.handler.hub.guild.HubGuildCreateHandler
import application.handler.hub.message.HubMessageCreateHandler
import cats.effect._
import cats.effect.std.{AtomicCell, Queue}
import cats.implicits.catsSyntaxParallelSequence1
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
        (for {
          applicationContext <- AtomicCell[IO].of(
            ApplicationContext(
              discordBotContext = BotContext.Init(config),
              hubContext = HubContext.empty
            )
          )
          // todo こいつどうにかしたほうがいい
          hubMessageService = new HubMessageService(transactor)
          messageQueue <- Queue.unbounded[IO, Payload[Json]]
          gatewayClient = GatewayClient.apply(messageQueue)(config)
          applicationStream = Stream.fromQueueUnterminated(messageQueue).evalTap { payload =>
            (for {
              d <- payload.d
              t <- payload.t
              handle <- DiscordEvent.fromString(t).collect {
                case DiscordEvent.Ready => GatewayReadyHandler.handle(d)
                case DiscordEvent.GuildCreate => HubGuildCreateHandler.handle(d)
                case DiscordEvent.MessageCreate => HubMessageCreateHandler.handle(d)(hubMessageService)
                case DiscordEvent.MessageUpdate => ???
                case DiscordEvent.MessageDelete => ???
                case DiscordEvent.ChannelCreate => ???
                case DiscordEvent.ChannelUpdate => ???
                case DiscordEvent.ChannelDelete => ???
                case DiscordEvent.ThreadCreate => ???
                case DiscordEvent.ThreadDelete => ???
              }
            } yield handle.run(applicationContext)).getOrElse(IO.unit)
          }
          _ <- List(
            gatewayClient,
            applicationStream.compile.drain
          ).parSequence
        } yield ()).as(ExitCode.Success)
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
