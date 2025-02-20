package berner.database.writer

import berner.model.hub.HubMessageMapping
import scalikejdbc._

import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object HubMessageMappingWriter {
  private val column = HubMessageMapping.column

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
