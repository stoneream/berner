package discord

import application.Config
import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import discord.payload.{Identity, Payload}
import fs2.{Pipe, Stream}
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSConnectionHighLevel, WSDataFrame, WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient

import scala.concurrent.duration.*

// todo logging
object GatewayClient {

  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  def run(config: Config): IO[Unit] = {
    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          jobQueue <- Queue.unbounded[IO, Json]
          _ <- sendIdentity(connection, config.discordToken)
          interval <- receiveHeartbeatInterval(connection)
          _ <- (
            sendHeartbeat(connection, interval),
            offerReceiveEvent(connection, jobQueue),
            Stream.fromQueueUnterminated(jobQueue).through(handleReceiveEvent).compile.drain
          ).parTupled
        } yield ()
      }
    }
  }

  /**
   * `Gateway API`へ接続する
   */
  private def sendIdentity(connection: WSConnectionHighLevel[IO], token: String): IO[Unit] = {
    val payload = Payload(GatewayOpCode.Identify, Some(Identity(token, 513, Map())), None, None)
    connection.send(WSFrame.Text(payload.asJson.noSpaces))
  }

  /**
   * `heartbeat interval`を受け取るまで待つ
   * ただし、30秒でタイムアウトする
   */
  private def receiveHeartbeatInterval(connection: WSConnectionHighLevel[IO]): IO[Int] = {
    connection.receiveStream
      .collectFirst({ case WSFrame.Text(text, _) =>
        parse(text)
      })
      .collectFirst({ case Right(json) =>
        json.hcursor.downField("d").downField("heartbeat_interval").as[Int]
      })
      // opcodeのチェックをしたほうが丁寧だけどやってない
      .collectFirst({ case Right(interval) =>
        interval
      })
      .timeout(30.seconds)
      .compile
      .lastOrError
  }

  /**
   * `heartbeat`を送信し続ける
   */
  private def sendHeartbeat[F[_] : Async](connection: WSConnectionHighLevel[F], interval: Int): F[Unit] = {
    Stream
      .awakeEvery(interval.millis)
      .evalMap { _ =>
        connection.send(WSFrame.Text("""{"op": 1, "d": null}"""))
      }
      .compile
      .drain
  }

  /**
   * `Gateway API`から受信したイベントをジョブキューに詰める
   */
  private def offerReceiveEvent(connection: WSConnectionHighLevel[IO], jobQueue: Queue[IO, Json]): IO[Unit] = {
    connection.receiveStream.collect({ case WSFrame.Text(text, _) => parse(text) }).collect({ case Right(json) => json }).evalMap(jobQueue.offer).compile.drain
  }

  /**
   * ジョブキューを処理する
   */
  private def handleReceiveEvent: Pipe[IO, Json, Unit] = _.evalMap { json =>
    IO.println(json.noSpaces)
  }

}
