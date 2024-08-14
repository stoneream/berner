package berner.bot

import berner.logging.Logger
import net.dv8tion.jda.api.JDABuilder
import com.typesafe.config.ConfigFactory
import net.dv8tion.jda.api.requests.GatewayIntent
import scalikejdbc.config.DBs

import scala.annotation.tailrec
import scala.util.control.Exception.allCatch

object Main extends App with Logger {
  try {
    val config = ConfigFactory.load()

    DBs.setupAll()

    val discordBotToken = config.getString("discord.token")

    @tailrec
    def infinite(): Unit = {
      allCatch.either {
        val api = JDABuilder
          .createDefault(discordBotToken)
          .enableIntents(GatewayIntent.MESSAGE_CONTENT)
          .enableIntents(GatewayIntent.GUILD_WEBHOOKS)
          .enableIntents(GatewayIntent.GUILD_MESSAGES)
          .addEventListeners(new Hub)
          .addEventListeners(new Archiver)
          .addEventListeners(new Ping)
          .build()

        api.awaitShutdown()
      } match {
        case Right(_) => // do nothing
        case Left(e) =>
          error("Botが異常終了", e)
          info("3秒後に再起動")
          Thread.sleep(3000)
          infinite()
      }
    }

    infinite()
  } finally {
    DBs.closeAll()
  }

}
