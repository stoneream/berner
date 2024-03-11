package berner.bot

import net.dv8tion.jda.api.JDABuilder
import com.typesafe.config.ConfigFactory
import net.dv8tion.jda.api.requests.GatewayIntent
import scalikejdbc.config.DBs

object Main extends App {
  try {
    val config = ConfigFactory.load()

    DBs.setupAll()

    val discordBotToken = config.getString("discord.token")

    val api = JDABuilder
      .createDefault(discordBotToken)
      .enableIntents(GatewayIntent.MESSAGE_CONTENT)
      .enableIntents(GatewayIntent.GUILD_WEBHOOKS)
      .enableIntents(GatewayIntent.GUILD_MESSAGES)
      .addEventListeners(new Hub)
      .addEventListeners(new Ping)
      .build()

    api.awaitReady()

  } finally {}

}
