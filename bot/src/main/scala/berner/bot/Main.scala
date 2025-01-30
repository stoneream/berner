package berner.bot

import berner.daemon.message_delete_daemon.MessageDeleteDamon
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
//  private val config = ConfigFactory.load()
//  private val discordBotToken = config.getString("discord.token")
//
//  val api = JDABuilder
//    .createDefault(discordBotToken)
//    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
//    .enableIntents(GatewayIntent.GUILD_WEBHOOKS)
//    .enableIntents(GatewayIntent.GUILD_MESSAGES)
//    .addEventListeners(new Hub)
//    .addEventListeners(new Archiver)
//    .addEventListeners(new Ping)
//    .build()
//
//  try {
//    DBs.setupAll()
//    api.awaitShutdown()
//  } finally {
//    DBs.closeAll()
//
//    val client = api.getHttpClient
//    client.connectionPool.evictAll()
//    client.dispatcher.executorService.shutdown()
//  }

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      messageDeleteDaemonFiber <- MessageDeleteDamon.task(1).start
      _ <- messageDeleteDaemonFiber.join
    } yield {
      ExitCode.Success
    }
  }
}
