package discord.handler

import cats.effect.IO
import discord.BotContext
import io.circe.Json
import io.circe.optics.JsonPath.root

object ThreadCreateHandler {
  def handle(json: Json)(context: BotContext): IO[BotContext] = {
    context match {
      case BotContext.InitializedBotContext(_) => IO.raiseError(new Exception("unexpected bot context"))
      case botContext @ BotContext.ReadyBotContext(_, times, timesThreads, _) =>
        val namePath = root.d.name.string
        val idPath = root.d.id.string
        val parentIdPath = root.d.parent_id.string

        val threadOpt = for {
          name <- namePath.getOption(json)
          id <- idPath.getOption(json)
          parent_id <- parentIdPath.getOption(json)
        } yield {
          BotContext.Thread(name, id, parent_id)
        }

        threadOpt match {
          case Some(createdThread) if times.exists(_.id == createdThread.parentId) =>

            IO(botContext.copy(timesThreads = createdThread :: timesThreads))
          case _ => IO(botContext)
        }
    }
  }
}
