package berner.feature.init_slash_command

import berner.feature.archiver.ArchiverListenerAdapter
import berner.feature.register_key.RegisterKeyListenerAdapter
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands

class InitSlashCommandListenerAdapter extends ListenerAdapter {
  override def onReady(event: ReadyEvent): Unit = {
    super.onReady(event)

    val jda = event.getJDA

    jda
      .updateCommands()
      .addCommands(
        Commands.slash(RegisterKeyListenerAdapter.slashCommandName, RegisterKeyListenerAdapter.slashCommandDescription).setGuildOnly(true),
        Commands.slash(ArchiverListenerAdapter.slashCommandName, ArchiverListenerAdapter.slashCommandDescription).setGuildOnly(true)
      )
      .queue()
  }

}
