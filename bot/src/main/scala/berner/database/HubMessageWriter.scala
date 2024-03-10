package berner.database

import berner.model.hub.HubMessage
import scalikejdbc._

import java.time.OffsetDateTime

object HubMessageWriter {
  def write(hm: HubMessage)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    sql"""
      INSERT INTO hub_messages (
        source_message_id,
        source_channel_id,
        message_id,
        channel_id,
        guild_id,
        created_at,
        updated_at,
        deleted_at
      ) VALUES (
        ${hm.sourceMessageId},
        ${hm.sourceChannelId},
        ${hm.messageId},
        ${hm.channelId},
        ${hm.guildId},
        ${hm.createdAt},
        ${hm.updatedAt},
        ${hm.deletedAt}
      )
   """.update.apply()
  }

  def delete(id: Long, now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    val hm = HubMessageSyntax.syntax("hm")
    withSQL {
      update(HubMessageSyntax as hm)
        .set(
          hm.deletedAt -> now,
          hm.updatedAt -> now
        )
        .where
        .eq(hm.id, id)
    }.update.apply()
  }

}
