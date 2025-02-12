package berner.daemon.register_key_daemon

import berner.feature.register_key.RegisterKeyListenerAdapter
import cats.effect.IO
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.{JDA, JDABuilder}

object RegisterKeyDaemon {
  def task(discordBotToken: String): IO[Unit] = {
    (for {
      jda <- preExecute(discordBotToken)
      _ <- execute(jda)
      _ <- postExecute()
    } yield ()).foreverM
  }

  private def preExecute(discordBotToken: String): IO[JDA] = IO {
    JDABuilder
      .createDefault(discordBotToken)
      .enableIntents(GatewayIntent.MESSAGE_CONTENT)
      .enableIntents(GatewayIntent.GUILD_MESSAGES)
      .addEventListeners(new RegisterKeyListenerAdapter)
      .build()
  }

  private def execute(jda: JDA): IO[Boolean] = {
    IO {
      jda
        .updateCommands()
        .addCommands(
          Commands.slash(RegisterKeyListenerAdapter.slashCommandName, RegisterKeyListenerAdapter.slashCommandDescription).setGuildOnly(true)
        )
        .queue()

      jda.awaitShutdown()
    }.guarantee(IO {
      val client = jda.getHttpClient
      client.connectionPool.evictAll()
      client.dispatcher.executorService.shutdown()
    })
  }

  private def postExecute(): IO[Unit] = IO {}
}
