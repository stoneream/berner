package berner.bot

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class Ping extends ListenerAdapter {
  override def onMessageReceived(event: MessageReceivedEvent): Unit = {
    if (event.getAuthor.isBot || event.isWebhookMessage) {
      // do nothing
    } else {
      event.getChannel.sendMessage("pong!").queue()
    }
  }
}
