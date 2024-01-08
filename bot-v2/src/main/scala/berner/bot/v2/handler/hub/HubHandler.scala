package berner.bot.v2.handler.hub

import ackcord.gateway.{Context, DispatchEventProcess, GatewayIntents, GatewayProcessHandler}
import ackcord.gateway.data.GatewayDispatchEvent
import berner.bot.v2.model.hub.HubContext
import cats.data.ReaderT
import cats.effect.IO
import cats.effect.std.{AtomicCell, Queue}
import fs2.{Pipe, Stream}

class HubHandler(
    hubContext: AtomicCell[IO, HubContext]
) extends GatewayProcessHandler.Base[IO]("hub-handler")
    with DispatchEventProcess[IO] {
  private type HubContextState = AtomicCell[IO, HubContext]

  private type Handler[E] = ReaderT[IO, (HubContextState, E), Unit]

  private def guildCreateHandler: Handler[GatewayDispatchEvent.GuildCreate] = ReaderT { case (context, event) =>
    context.evalGetAndUpdate { _ => ??? }.void
  }

  private def messageCreateHandler: Handler[GatewayDispatchEvent.MessageCreate] = ReaderT { case (context, event) =>
    context.get.flatMap {
      case HubContext.Uninitialized() => ???
      case HubContext.Initialized() => ???
    }
  }

  override def onDispatchEvent(event: GatewayDispatchEvent, context: Context): IO[Unit] = {
    for {
      result <- event match {
        case event: GatewayDispatchEvent.GuildCreate => guildCreateHandler((hubContext, event))
        case event: GatewayDispatchEvent.MessageCreate => messageCreateHandler((hubContext, event))
        case _ => IO.unit
      }
    } yield result
  }
}
