package discord

import cats.effect._
import cats.effect.std.Queue
import cats.implicits._
import discord.BotContext.Init
import discord.payload.Identity.Intent
import discord.payload.{GatewayOpCode, Identity, Payload}
import fs2.Stream
import io.circe.Json
import io.circe.generic.auto._
import io.circe.optics.JsonPath._
import io.circe.parser._
import io.circe.syntax._
import org.http4s.client.websocket.{WSConnectionHighLevel, WSFrame, WSRequest}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.jdkhttpclient.JdkWSClient
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration._

object GatewayClient {
  private val logger = Slf4jLogger.getLogger[IO]

  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  def apply(messageQueue: Queue[IO, Payload[Json]])(config: DiscordConfig): IO[Unit] = {
    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          _ <- sendIdentity(connection, config)
          interval <- receiveHeartbeatInterval(connection)
          _ <- (
            sendHeartbeat(connection, interval),
            offerReceiveEvent(connection, messageQueue)
          ).parTupled
        } yield ()
      }
    }
  }

  /**
   * `Gateway API`へ接続する
   */
  private def sendIdentity(connection: WSConnectionHighLevel[IO], config: DiscordConfig): IO[BotContext] = {
    val intents = Seq(
      Intent.GUILDS,
      Intent.GUILD_MEMBERS,
      Intent.GUILD_MESSAGES,
      Intent.GUILD_MESSAGE_REACTIONS,
      Intent.MESSAGE_CONTENT
    )
    val identity = Identity(config.token, intents)
    val payload = Payload(GatewayOpCode.Identify, Some(identity), None, None)

    for {
      _ <- connection.send(WSFrame.Text(payload.asJson.noSpaces))
    } yield {
      Init(config)
    }
  }

  /**
   * `heartbeat interval`を受け取るまで待つ
   * ただし、30秒でタイムアウトする
   */
  private def receiveHeartbeatInterval(connection: WSConnectionHighLevel[IO]): IO[Int] = {
    connection.receiveStream
      .collectFirst({ case WSFrame.Text(text, _) => parse(text) })
      .collectFirst({ case Right(json) =>
        root.d.heartbeat_interval.int.getOption(json)
      })
      // opcodeのチェックをしたほうが丁寧だけどやってない
      .collectFirst({ case Some(interval) => interval })
      .timeout(30.seconds)
      .compile
      .lastOrError
  }

  /**
   * `heartbeat`を送信し続ける
   */
  private def sendHeartbeat[F[_]: Async](connection: WSConnectionHighLevel[F], interval: Int): F[Unit] = {
    Stream
      .awakeEvery(interval.millis)
      .evalMap { _ =>
        val payload = WSFrame.Text("""{"op": 1, "d": null}""")
        connection.send(payload)
      }
      .compile
      .drain
  }

  /**
   * `Gateway API`から受信したイベントをジョブキューに詰める
   */
  private def offerReceiveEvent(connection: WSConnectionHighLevel[IO], jobQueue: Queue[IO, Payload[Json]]): IO[Unit] = {
    connection.receiveStream
      .evalTap({ case WSFrame.Text(text, _) => logger.debug(text) })
      .collect({ case WSFrame.Text(text, _) => decode[Payload[Json]](text) })
      .collect({ case Right(payload) => payload })
      .evalMap({ payload => jobQueue.offer(payload) })
      .compile
      .drain
  }
}
