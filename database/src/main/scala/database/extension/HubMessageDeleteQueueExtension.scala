package database.extension

import scalikejdbc._
import database.{HubMessageDeleteQueue, HubMessageMapping}

import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object HubMessageDeleteQueueExtension {
  private val hmdq = HubMessageDeleteQueue.hmdq
  private val hmm = HubMessageMapping.hmm

  private val column = HubMessageDeleteQueue.column

  def pendings(limit: Int)(session: DBSession): List[(HubMessageDeleteQueue, HubMessageMapping)] = {
    implicit val s: DBSession = session
    withSQL {
      select
        .from(HubMessageDeleteQueue as hmdq)
        .join(HubMessageMapping as hmm)
        .on(sqls.eq(hmdq.hubMessageMappingId, hmm.id).and.isNull(hmm.deletedAt))
        .where
        .eq(hmdq.status, 0) // 削除待ち
        .and
        .eq(hmdq.deletedAt, null)
        .limit(limit)
    }.map(rs => (HubMessageDeleteQueue(hmdq.resultName)(rs), HubMessageMapping(hmm.resultName)(rs))).list.apply()
  }

  def write(rows: List[HubMessageDeleteQueue])(session: DBSession): Unit = {
    implicit val s: DBSession = session

    val builder = BatchParamsBuilder {
      rows.map { row =>
        Seq(
          column.hubMessageMappingId -> row.hubMessageMappingId,
          column.status -> row.status,
          column.createdAt -> row.createdAt,
          column.updatedAt -> row.updatedAt,
          column.deletedAt -> row.deletedAt
        )
      }
    }

    withSQL {
      insert.into(HubMessageDeleteQueue).namedValues(builder.columnsAndPlaceholders: _*)
    }.batch(builder.batchParams: _*).apply()
  }

  def markDeleteByMessageIds(ids: List[Long], now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session

    withSQL {
      update(HubMessageDeleteQueue)
        .set(
          column.status -> 1, // 削除済み
          column.updatedAt -> now
        )
        .where
        .in(column.id, ids)
    }.update.apply()
  }

  def markFailedByMessageIds(ids: List[Long], now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session

    withSQL {
      update(HubMessageDeleteQueue)
        .set(
          column.status -> 2, // 削除失敗
          column.updatedAt -> now
        )
        .where
        .in(column.id, ids)
    }.update.apply()
  }
}
