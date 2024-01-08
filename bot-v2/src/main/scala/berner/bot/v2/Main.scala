package berner.bot.v2

import ackcord.BotSettings
import ackcord.gateway.data.{GatewayDispatchEvent, GatewayEventBase}
import ackcord.gateway.{Context, DispatchEventProcess, GatewayIntents, GatewayProcessHandler}
import berner.bot.v2.handler.hub.HubHandler
import berner.bot.v2.model.hub.HubContext
import cats.effect.std.AtomicCell
import cats.effect.{ExitCode, IO, IOApp}
import sttp.client3.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val app =
      for {
        hubContext <- AtomicCell[IO].of[HubContext](HubContext.Uninitialized())
        _ <- BotSettings
          .cats(
            token = "",
            intents = GatewayIntents.values.foldLeft(GatewayIntents.unknown(0))(_ ++ _),
            sttpBackend = HttpClientCatsBackend.resource[IO]()
          )
          .map { s =>
            import s.doAsync
            s.copy(logGatewayMessages = true)
          }
          .map { settings =>
            settings.install(new HubHandler(hubContext))
          }
          .use(_.start)
      } yield ()

    app.as(ExitCode.Success)
  }
}
