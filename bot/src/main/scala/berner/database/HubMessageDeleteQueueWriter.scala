package berner.database

import berner.model.hub.{HubMessageDeleteQueue, HubMessageMapping}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object HubMessageDeleteQueueWriter {
  private val column = HubMessageDeleteQueue.column

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
      insert.into(HubMessageMapping).namedValues(builder.columnsAndPlaceholders: _*)
    }.batch(builder.batchParams: _*).apply()
  }

  def markDeleteByMessageIds(ids: List[Long], now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session

    withSQL {
      update(HubMessageDeleteQueue)
        .set(column.status -> 1) // 削除済み
        .set(column.updatedAt -> now)
        .where
        .in(column.id, ids)
    }.update.apply()
  }

  def markFailedByMessageIds(ids: List[Long], now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session

    withSQL {
      update(HubMessageDeleteQueue)
        .set(column.status -> 2) // 削除失敗
        .set(column.updatedAt -> now)
        .where
        .in(column.id, ids)
    }.update.apply()
  }

}
