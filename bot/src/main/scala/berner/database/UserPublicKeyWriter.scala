package berner.database

import berner.model.hub.HubMessageMapping
import berner.model.register_key.UserPublicKey
import scalikejdbc._

import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object UserPublicKeyWriter {
  private val column = UserPublicKey.column

  def write(rows: List[UserPublicKey])(session: DBSession): Unit = {
    implicit val s: DBSession = session

    val builder = BatchParamsBuilder {
      rows.map { row =>
        Seq(
          column.userId -> row.userId,
          column.keyPem -> row.keyPem,
          column.keyType -> row.keyType,
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
