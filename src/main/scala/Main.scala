import cats.effect.*
import fs2.Stream
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSClientHighLevel, WSConnectionHighLevel, WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient

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

  private def handleHeartbeat(client: WSConnectionHighLevel[IO], interval: FiniteDuration): Stream[IO, Unit] = {
    val heartbeat = WSFrame.Text("""{"op": 1, "d": null}""")
    Stream.awakeEvery[IO](interval).evalMap { _ => client.send(heartbeat) }
  }

  private def initializeClient(config: Config): IO[Unit] = {
    JdkWSClient[IO](HttpClient.newHttpClient).connectHighLevel(WSRequest(gatewayUri)).use { client =>
      val identity = Payload[Identity](2, Some(Identity(config.discordToken, 513, Map())), None, None)
      val payload = WSFrame.Text(identity.asJson.noSpaces)

      client.send(payload).flatMap { _ =>
        client.receiveStream
          .collectFirst({ case WSFrame.Text(text, _) => text })
          .flatMap { text =>
            parse(text) match {
              case Left(parsingFailure) => throw Exception("failed to parse json", parsingFailure)
              case Right(json) =>
                val op = json.hcursor.downField("op").as[Int].getOrElse(-1)
                Stream.emit(json).covary[IO].evalTap(json => IO(println(json.noSpaces))).flatMap { _ =>
                  op match {
                    case GatewayOpCode.Hello =>
                      val interval =
                        json.hcursor.downField("d").downField("heartbeat_interval").as[Int].getOrElse(throw Exception("heartbeat_interval is not found"))
                      handleHeartbeat(client, interval.millis).concurrently(client.receiveStream.evalMap {
                        case WSFrame.Text(text, _) => IO.println(text)
                        case _ => IO.unit
                      })
                    case op =>
                      throw Exception(s"unexpected operation code (op=$op)")
                  }
                }
            }
          }
          .compile
          .drain
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

  private case class Payload[T](op: Int, d: Option[T], s: Option[Int], t: Option[String])

  private case class Identity(token: String, intents: Int, properties: Map[String, String])

  // https://discord.com/developers/docs/topics/opcodes-and-status-codes#gateway
  private object GatewayOpCode {
    val Dispatch = 0
    val Heartbeat = 1
    val Identify = 2
    val PresenceUpdate = 3
    val VoiceStateUpdate = 4
    val Resume = 6
    val Reconnect = 7
    val RequestGuildMembers = 8
    val InvalidSession = 9
    val Hello = 10
    val HeartbeatAck = 11
  }
}
