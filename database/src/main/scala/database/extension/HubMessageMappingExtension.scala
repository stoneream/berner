package database.extension

import database.HubMessageMapping
import scalikejdbc._
import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object HubMessageMappingExtension {
  private val hmm = HubMessageMapping.hmm
  private val column = HubMessageMapping.column

  def find(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: Option[String],
      sourceMessageId: String,
      guildId: String
  )(session: DBSession): Option[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .eq(hmm.sourceMessageId, sourceMessageId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).single.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      guildId: String
  )(session: DBSession): List[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).list.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: String,
      guildId: String
  )(session: DBSession): List[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).list.apply()
  }

  def write(rows: List[HubMessageMapping])(session: DBSession): Unit = {
    implicit val s: DBSession = session

    val builder = BatchParamsBuilder {
      rows.map { row =>
        Seq(
          column.guildId -> row.guildId,
          column.sourceGuildMessageChannelId -> row.sourceGuildMessageChannelId,
          column.sourceThreadMessageChannelId -> row.sourceThreadMessageChannelId,
          column.sourceMessageId -> row.sourceMessageId,
          column.hubGuildMessageChannelId -> row.hubGuildMessageChannelId,
          column.hubMessageId -> row.hubMessageId,
          column.createdAt -> row.createdAt,
          column.updatedAt -> row.updatedAt,
          column.deletedAt -> row.deletedAt
        )
      }
    }

    withSQL {
      insert.into(HubMessageMapping).namedValues(builder.columnsAndPlaceholders: _*)
    }.batch(builder.batchParams: _*).apply()
  }

  def delete(id: Long, now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    val hmm = HubMessageMapping.syntax("hmm")
    withSQL {
      update(HubMessageMapping as hmm)
        .set(
          hmm.deletedAt -> now,
          hmm.updatedAt -> now
        )
        .where
        .eq(hmm.id, id)
    }.update.apply()
  }

}
