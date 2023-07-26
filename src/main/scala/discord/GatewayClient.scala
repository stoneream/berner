package discord

import application.Config
import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.Queue
import discord.payload.{Identity, Payload}
import fs2.Pipe
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSConnectionHighLevel, WSDataFrame, WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient
import fs2.Stream
import scala.concurrent.duration.*

object GatewayClient {

  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  /**
   * `Gateway API`へ接続する
   */
  private def sendIdentity[F[_]: Async](connection: WSConnectionHighLevel[F], token: String): F[Unit] = {
    val payload = Payload(GatewayOpCode.Identify, Some(Identity(token, 513, Map())), None, None)
    connection.send(WSFrame.Text(payload.asJson.noSpaces))
  }

  /**
   * `heartbeat interval`を受け取るまで待つ
   * ただし、30秒でタイムアウトする
   */
  private def receiveHeartbeatInterval[F[_]: Async](connection: WSConnectionHighLevel[F]): F[Int] = {
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

  private def sendHeartbeat[F[_]: Async](connection: WSConnectionHighLevel[F], interval: Int): F[Unit] = {
    Stream
      .awakeEvery[F](interval.millis)
      .evalMap { _ =>
        connection.send(WSFrame.Text("""{"op": 1, "d": null}"""))
      }
      .compile
      .drain
  }

  private def handleReceiveEvent = ???

  def run(config: Config): IO[Unit] = {
    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          _ <- sendIdentity(connection, config.discordToken)
          interval <- receiveHeartbeatInterval(connection)
          _ <- (
            sendHeartbeat(connection, interval),
            handleReceiveEvent
          ).parTupled
        } yield ()
      }
    }
  }
}
