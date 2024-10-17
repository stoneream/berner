package berner.bot

import berner.logging.Logger
import com.typesafe.config.ConfigFactory
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import scalikejdbc.config.DBs

object Main extends App with Logger {
  private val config = ConfigFactory.load()
  private val discordBotToken = config.getString("discord.token")

  val api = JDABuilder
    .createDefault(discordBotToken)
    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
    .enableIntents(GatewayIntent.GUILD_WEBHOOKS)
    .enableIntents(GatewayIntent.GUILD_MESSAGES)
    .addEventListeners(new Hub)
    .addEventListeners(new Archiver)
    .addEventListeners(new Ping)
    .build()

  try {
    DBs.setupAll()
    api.awaitShutdown()
  } finally {
    DBs.closeAll()

    val client = api.getHttpClient
    client.connectionPool.evictAll()
    client.dispatcher.executorService.shutdown()
  }
}
