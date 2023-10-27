package berner.bot.service

import berner.core.model.HubMessage
import cats.effect.Sync
import doobie.util.transactor.Transactor
import doobie.implicits._
import doobie.implicits.javatimedrivernative._

import java.time.LocalDateTime

// todo ExchangeRateWriterみたいに切り出して～～
class HubMessageService[F[_]: Sync](transactor: Transactor[F]) {

  def find(sourceMessageId: String, sourceChannelId: String, guildId: String): F[Option[HubMessage]] = {
    sql"""
      SELECT
      id,
      source_message_id,
      source_channel_id,
      message_id,
      channel_id,
      guild_id,
      created_at,
      updated_at,
      deleted_at
      FROM hub_messages
      WHERE
      deleted_at IS NULL AND
      source_message_id = $sourceMessageId AND
      source_channel_id = $sourceChannelId AND
      guild_id = $guildId
    """.query[HubMessage].option.transact(transactor)
  }

  def findBySourceChannelId(sourceChannelId: String, guildId: String): F[List[HubMessage]] = {
    sql"""
      SELECT
      id,
      source_message_id,
      source_channel_id,
      message_id,
      channel_id,
      guild_id,
      created_at,
      updated_at,
      deleted_at
      FROM hub_messages
      WHERE
      deleted_at IS NULL AND
      source_channel_id = $sourceChannelId AND
      guild_id = $guildId
    """.query[HubMessage].to[List].transact(transactor)
  }

  def write(hb: HubMessage): F[Int] = {
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
       ${hb.sourceMessageId},
       ${hb.sourceChannelId},
       ${hb.messageId},
       ${hb.channelId},
       ${hb.guildId},
       ${hb.createdAt},
       ${hb.updatedAt},
       ${hb.deletedAt}
      )
      """.update.run.transact(transactor)
  }

  def delete(id: Long, now: LocalDateTime): F[Int] = {
    sql"""
         UPDATE hub_messages
         SET deleted_at = $now
         WHERE id = $id
      """.update.run.transact(transactor)
  }

}
