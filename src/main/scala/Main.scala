import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import discord.GatewayOpCode
import discord.payload.{Identity, Payload}
import fs2.{Pipe, Stream}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSClientHighLevel, WSConnectionHighLevel, WSDataFrame, WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient
import org.http4s.websocket.WebSocketFrame

import java.net.http.HttpClient
import scala.concurrent.duration.*

object Main extends IOApp {
  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  override def run(args: List[String]): IO[ExitCode] = {
    Config.apply.use { config =>
      reconnect(0, config).as(ExitCode.Success)
    }
  }

  private def handleHeartbeat(connection: WSConnectionHighLevel[IO], interval: FiniteDuration): IO[Unit] = {
    val heartbeat = WSFrame.Text("""{"op": 1, "d": null}""")
    Stream.awakeEvery[IO](interval).evalMap { _ => connection.send(heartbeat) }.compile.drain
  }

  private def initializeClient(config: Config): IO[Unit] = {
    val identity = Payload[Identity](GatewayOpCode.Identify, Some(Identity(config.discordToken, 513, Map())), None, None)
    val payload = WSFrame.Text(identity.asJson.noSpaces)

    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          queue <- Queue.unbounded[IO, WSDataFrame]
          _ <- connection.send(payload)
          interval <-
            connection.receiveStream
              .collectFirst({ case WSFrame.Text(text, _) => text })
              .evalMap { text =>
                // opのチェックくらいはしたほうが丁寧だけど面倒なので後回し
                IO.fromEither {
                  parse(text).map { json => json.hcursor.downField("d").downField("heartbeat_interval").as[Int] }
                }.flatMap {
                  case Right(heartbeatInterval) => IO.pure(heartbeatInterval)
                  case _ => IO.raiseError(Exception("failed to get heartbeat interval"))
                }
              }
              .timeout(30.seconds)
              .compile
              .lastOrError
          _ <- (
            connection.receiveStream
              .through(_.evalMap {
                case text: WSFrame.Text => queue.offer(text).as(None)
                case _: WSFrame.Binary => IO.pure(None) // バイナリは特にハンドリングしない
              })
              .compile
              .drain,
            handleHeartbeat(connection, interval.millis),
            Stream.fromQueueUnterminated(queue).through(_.evalMap(event => IO.println(event))).compile.drain
          ).parTupled
        } yield ()
      }
    }
  }

  private def reconnect(attempt: Int, config: Config): IO[Unit] = {
    if (attempt > 5) {
      IO.raiseError(Exception("failed to connect"))
    } else {
      initializeClient(config).handleErrorWith { err =>
        for {
          _ <- IO.println(err)
          _ <- IO.println(s"failed to connect (attempt=$attempt)")
          _ <- IO.sleep(5.seconds)
          _ <- reconnect(attempt + 1, config)
        } yield {}
      }
    }
  }
}
