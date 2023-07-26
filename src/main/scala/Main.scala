import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import discord.GatewayOpCode
import discord.payload.{Identity, Payload}
import fs2.Stream
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSClientHighLevel, WSConnectionHighLevel, WSDataFrame, WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient
import org.http4s.websocket.WebSocketFrame
import org.typelevel.log4cats.*
import org.typelevel.log4cats.slf4j.Slf4jFactory

import java.net.http.HttpClient
import scala.concurrent.duration.*

object Main extends IOApp {
  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  private implicit val logging: LoggerFactory[IO] = Slf4jFactory.create[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Config.apply.use { config =>
      reconnect(0, config).as(ExitCode.Success)
    }
  }

  // todo Bot起動前にデータベースを初期化する
  // todo 処理を関数に切り出す
  private def discordBot(config: Config): IO[Unit] = {
    val logger = LoggerFactory.getLogger

    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          queue <- Queue.unbounded[IO, WSDataFrame]
          // Identifyを送信
          _ <- connection.send(WSFrame.Text(Payload(GatewayOpCode.Identify, Some(Identity(config.discordToken, 513, Map())), None, None).asJson.noSpaces))
          // `heartbeat_interval`を受け取るまで待つ
          // ただし、30秒でタイムアウト
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
            // イベントが発生するたびにジョブキューに詰める
            connection.receiveStream
              .through(_.evalMap {
                case text: WSFrame.Text => queue.offer(text).as(None)
                case _: WSFrame.Binary => IO.pure(None) // バイナリは特にハンドリングしない
              })
              .compile
              .drain,
            // ジョブキューから取り出して処理
            // todo コマンド定義と各種処理の実装など
            Stream.fromQueueUnterminated(queue).through(_.evalMap(event => logger.info(event.toString))).compile.drain,
            // heartbeatを送信し続ける
            Stream.awakeEvery[IO](interval.millis).evalMap { _ => connection.send(WSFrame.Text("""{"op": 1, "d": null}""")) }.compile.drain
          ).parTupled
        } yield ()
      }
    }
  }

  private def reconnect(attempt: Int, config: Config): IO[Unit] = {
    val logger = LoggerFactory.getLogger

    if (attempt > 5) {
      IO.raiseError(Exception("failed to connect"))
    } else {
      discordBot(config).handleErrorWith { err =>
        for {
          _ <- logger.error(err)(s"failed to connect (attempt=$attempt)")
          _ <- IO.sleep(5.seconds)
          _ <- reconnect(attempt + 1, config)
        } yield {}
      }
    }
  }
}
