package berner.database

import berner.model.hub.HubMessageMapping
import scalikejdbc._

import java.time.OffsetDateTime

object HubMessageMappingWriter {
  def write(hmm: HubMessageMapping)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    sql"""
      INSERT INTO hub_message_mappings (
        guild_id,
        source_guild_message_channel_id,
        source_thread_message_channel_id,
        source_message_id,
        hub_guild_message_channel_id,
        hub_message_id,
        created_at,
        updated_at,
        deleted_at
      ) VALUES (
        ${hmm.guildId},
        ${hmm.sourceGuildMessageChannelId},
        ${hmm.sourceThreadMessageChannelId},
        ${hmm.sourceMessageId},
        ${hmm.hubGuildMessageChannelId},
        ${hmm.hubMessageId},
        ${hmm.createdAt},
        ${hmm.updatedAt},
        ${hmm.deletedAt}
     )
     """.update.apply()
  }

  def delete(id: Long, now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    val hmm = HubMessageMappingSyntax.syntax("hmm")
    withSQL {
      update(HubMessageMappingSyntax as hmm)
        .set(
          hmm.deletedAt -> now,
          hmm.updatedAt -> now
        )
        .where
        .eq(hmm.id, id)
    }.update.apply()
  }

}
