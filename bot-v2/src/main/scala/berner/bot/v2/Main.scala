package berner.bot.v2

import ackcord.BotSettings
import ackcord.gateway.GatewayIntents
import ackcord.gateway.data.GatewayDispatchEvent
import ackcord.interactions.Components
import cats.effect.{ExitCode, IO, Resource, ResourceApp}
import cats.syntax.all._
import sttp.client3.httpclient.cats.HttpClientCatsBackend

object Main extends ResourceApp {
  override def run(args: List[String]): Resource[IO, ExitCode] = {
    BotSettings
      .cats(
        token = "TOKEN_HERE",
        intents = GatewayIntents.values.foldLeft(GatewayIntents.unknown(0))(_ ++ _),
        sttpBackend = HttpClientCatsBackend.resource[IO]()
      )
      .map { s =>
        import s.doAsync
        s.copy(logGatewayMessages = true)
      }
      .mproduct { settings => Resource.eval(Components.ofCats[IO](settings.requests)) }
      .map { case (settings, _) =>
        settings.installEventListener {
          case _: GatewayDispatchEvent.Ready => IO.println("Ready!")
          case _: GatewayDispatchEvent.MessageCreate => IO.print("Received message!")
        }
      }
      .flatMap(_.startResource)
  }
}
