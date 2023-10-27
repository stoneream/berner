import berner.bot.handler.DoNothingHandler
import berner.bot.handler.gateway.GatewayReadyHandler
import berner.bot.handler.hub.HubContext
import berner.bot.handler.hub.guild.HubGuildCreateHandler
import berner.bot.handler.hub.message.{HubMessageCreateHandler, HubMessageDeleteHandler, HubMessageUpdateHandler}
import berner.bot.handler.hub.thread.{HubThreadCreateHandler, HubThreadDeleteHandler}
import berner.bot.handler.ping.PingHandler
import berner.bot.model.ApplicationContext
import berner.bot.service.HubMessageService
import berner.core.database.{Database, DatabaseConfig}
import cats.effect._
import cats.effect.std.{AtomicCell, Queue}
import cats.implicits.catsSyntaxParallelSequence1
import com.typesafe.config.ConfigFactory
import discord.payload.Payload
import discord.payload.Payload.DiscordEvent
import discord.{BotContext, DiscordConfig, GatewayClient}
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import fs2.Stream
import io.circe.Json
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jFactory

object Main extends IOApp {
  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    val logger = LoggerFactory.getLogger

    def loadConfig: IO[(DatabaseConfig, DiscordConfig)] = for {
      config <- IO(ConfigFactory.load())
    } yield {
      val databaseConfig = DatabaseConfig.fromConfig(config)
      val discordConfig = DiscordConfig.fromConfig(config)
      (databaseConfig, discordConfig)
    }

    def setupDB(databaseConfig: DatabaseConfig): Resource[IO, HikariTransactor[IO]] = {
      ExecutionContexts.fixedThreadPool[IO](databaseConfig.poolMaxSize).flatMap { ec =>
        Database.apply(databaseConfig, ec)
      }
    }

    def startBot(discordConfig: DiscordConfig, transactor: HikariTransactor[IO]): IO[Unit] = {
      for {
        applicationContext <- AtomicCell[IO].of(
          ApplicationContext(
            discordBotContext = BotContext.Init(discordConfig),
            hubContext = HubContext.empty
          )
        )
        hubMessageService = new HubMessageService(transactor)
        messageQueue <- Queue.unbounded[IO, Payload[Json]]
        gatewayClient = GatewayClient.apply(messageQueue)(discordConfig)
        applicationStream = Stream.fromQueueUnterminated(messageQueue).evalTap { payload =>
          val handlerOpt = for {
            d <- payload.d
            t <- payload.t
            handle <- DiscordEvent.fromString(t).map {
              case DiscordEvent.Ready => GatewayReadyHandler.handle(d)
              case DiscordEvent.GuildCreate => HubGuildCreateHandler.handle(d)
              case DiscordEvent.MessageCreate =>
                List(
                  HubMessageCreateHandler.handle(d)(hubMessageService),
                  PingHandler.handle(d)
                ).parSequence
              case DiscordEvent.MessageUpdate => HubMessageUpdateHandler.handle(d)(hubMessageService)
              case DiscordEvent.MessageDelete => HubMessageDeleteHandler.handle(d)(hubMessageService)
              /* todo impl
              case DiscordEvent.ChannelCreate => ???
              case DiscordEvent.ChannelUpdate => ???
              case DiscordEvent.ChannelDelete => ???
               */
              case DiscordEvent.ThreadCreate => HubThreadCreateHandler.handle(d)
              case DiscordEvent.ThreadDelete => HubThreadDeleteHandler.handle(d)(hubMessageService)
              case _ => DoNothingHandler.handle
            }
          } yield handle.run(applicationContext).attempt.flatMap {
            case Left(e) => logger.error(e)("failed to handle payload")
            case Right(_) => IO.unit
          }
          handlerOpt.getOrElse(IO.unit)
        }
        _ <- List(
          gatewayClient,
          applicationStream.compile.drain
        ).parSequence
      } yield ()
    }

    for {
      config <- loadConfig
      (databaseConfig, discordConfig) = config
      _ <- setupDB(databaseConfig).use(startBot(discordConfig, _))
    } yield ExitCode.Success
  }
}
