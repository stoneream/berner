package discord

import cats.effect._
import cats.effect.std.Queue
import cats.implicits._
import discord.BotContext.InitializedBotContext
import discord.payload.Identity.Intent
import discord.payload.{Identity, Payload}
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

  // 面倒だったのですべてのイベントを垂れ流すハンドラを引数に取るのみとしている
  def run(
      config: DiscordConfig,
      jobHandler: (BotContext, Json) => IO[BotContext]
  ): IO[Unit] = {
    JdkWSClient.simple[IO].flatMap { client =>
      client.connectHighLevel(WSRequest(gatewayUri)).use { connection =>
        for {
          jobQueue <- Queue.unbounded[IO, Json]
          initializedContext <- sendIdentity(connection, config.token)
          interval <- receiveHeartbeatInterval(connection)
          _ <- (
            sendHeartbeat(connection, interval),
            offerReceiveEvent(connection, jobQueue),
            handleReceiveEvent(jobQueue, jobHandler)(initializedContext)
          ).parTupled
        } yield ()
      }
    }
  }

  /**
   * `Gateway API`へ接続する
   */
  private def sendIdentity(connection: WSConnectionHighLevel[IO], token: String): IO[BotContext] = {
    val intents = Seq(
      Intent.GUILDS,
      Intent.GUILD_MEMBERS,
      Intent.GUILD_MESSAGES,
      Intent.GUILD_MESSAGE_REACTIONS,
      Intent.MESSAGE_CONTENT
    )
    val identity = Identity(token, intents)
    val payload = Payload(GatewayOpCode.Identify, Some(identity), None, None)

    for {
      _ <- connection.send(WSFrame.Text(payload.asJson.noSpaces))
    } yield {
      InitializedBotContext(token)
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
  private def offerReceiveEvent(connection: WSConnectionHighLevel[IO], jobQueue: Queue[IO, Json]): IO[Unit] = {
    connection.receiveStream
      .collect({ case WSFrame.Text(text, _) => decode[Json](text) })
      .collect({ case Right(json) => json })
      .evalMap({ json => logger.info(json.noSpaces) *> jobQueue.offer(json) })
      .compile
      .drain
  }

  /**
   * ジョブキューからイベントを受け取り、ハンドラに処理を委譲する
   */
  private def handleReceiveEvent(
      jobQueue: Queue[IO, Json],
      jobHandler: (BotContext, Json) => IO[BotContext]
  )(context: BotContext): IO[Unit] = {
    Stream
      .fromQueueUnterminated(jobQueue)
      .through(_.evalMapAccumulate(context) { case (context1, json) =>
        jobHandler(context1, json)
          .map { context2 =>
            (context2, ())
          }
          .handleErrorWith { e =>
            // ジョブキュー内で何らかのエラーが発生してもアプリケーション全体は落ちないようにする
            logger.error(e)("failed handle event") *> IO.pure((context1, ()))
          }
      })
      .compile
      .drain
  }
}
