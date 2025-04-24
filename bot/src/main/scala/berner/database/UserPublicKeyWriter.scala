package berner.database

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
      insert.into(UserPublicKey).namedValues(builder.columnsAndPlaceholders: _*)
    }.batch(builder.batchParams: _*).apply()
  }

  def deleteByUserId(userId: String, now: OffsetDateTime)(session: DBSession): Unit = {
    implicit val s: DBSession = session
    val upk = UserPublicKey.syntax("upk")
    withSQL {
      update(UserPublicKey as upk)
        .set(
          upk.deletedAt -> now,
          upk.updatedAt -> now
        )
        .where
        .eq(upk.userId, userId)
        .and
        .isNull(upk.deletedAt)
    }.update.apply()
  }
}
