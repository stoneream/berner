import cats.effect.*
import fs2.Stream
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.websocket.{WSFrame, WSRequest}
import org.http4s.implicits.uri
import org.http4s.jdkhttpclient.JdkWSClient

import java.net.http.HttpClient
import scala.concurrent.duration.*

object Main extends IOApp {
  // todo コンフィグファイルからトークンを読み込む
  private val token = "server-token-here"
  // todo gateway api 叩いて url を取得する
  private val gatewayUri = uri"wss://gateway.discord.gg/?v=10&encoding=json"

  // todo 再接続処理
  override def run(args: List[String]): IO[ExitCode] = {
    JdkWSClient[IO](HttpClient.newHttpClient)
      .connectHighLevel(WSRequest(gatewayUri))
      .use { client =>
        val identity = Payload[Identity](2, Some(Identity(token, 513, Map())), None, None)
        val payload = WSFrame.Text(identity.asJson.noSpaces)
        client.send(payload).flatMap { _ =>
          client.receiveStream
            .collectFirst({ case WSFrame.Text(text, _) => text })
            .flatMap { text =>
              parse(text) match {
                case Left(_) => Stream.unit
                case Right(json) =>
                  val op = json.hcursor.downField("op").as[Int].getOrElse(-1)
                  for {
                    _ <- Stream.emit(json).covary[IO].evalTap(json => IO.println(json.spaces2))
                    _ <- op match {
                      case 10 => // hello
                        val interval = json.hcursor.downField("d").downField("heartbeat_interval").as[Int].getOrElse(-1)
                        val heartbeat = WSFrame.Text("""{"op": 1, "d": null}""")
                        Stream.awakeEvery[IO](interval.millis).evalMap { _ => client.send(heartbeat) }
                      case _ =>
                        Stream.unit
                    }
                  } yield ()
              }
            }
            .compile
            .drain
        }
      }
      .as(ExitCode.Success)
  }

  private case class Payload[T](op: Int, d: Option[T], s: Option[Int], t: Option[String])

  private case class Identity(token: String, intents: Int, properties: Map[String, String])
}
