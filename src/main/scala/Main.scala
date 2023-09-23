import application.ApplicationContext
import application.ApplicationContext.ApplicationContextState
import application.handler.DoNothingHandler
import application.handler.gateway.GatewayReadyHandler
import application.handler.hub.HubContext
import application.handler.hub.guild.HubGuildCreateHandler
import application.handler.hub.message.{HubMessageCreateHandler, HubMessageDeleteHandler, HubMessageUpdateHandler}
import application.handler.hub.thread.{HubThreadCreateHandler, HubThreadDeleteHandler}
import application.handler.ping.PingHandler
import cats.data.{ReaderT, StateT}
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
        } yield ()).as(ExitCode.Success)
      }
    }
  }
}
