package berner.database

import berner.model.hub.{HubMessageDeleteQueue, HubMessageMapping}
import scalikejdbc._

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

}
