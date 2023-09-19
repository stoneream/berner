package application

import application.handler.hub.HubContext

case class ApplicationContext(
    discordBotContext: discord.BotContext,
    hubContext: HubContext
)
