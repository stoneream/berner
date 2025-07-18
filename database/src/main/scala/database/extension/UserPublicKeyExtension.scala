package database.extension

import database.UserPublicKey
import scalikejdbc._

import java.time.OffsetDateTime
import scala.collection.IndexedSeq.iterableFactory

object UserPublicKeyExtension {
  private val upk = UserPublicKey.upk
  private val column = UserPublicKey.column

  sealed abstract class UserPublicKeyAlgorithm(val value: String)

  object UserPublicKeyAlgorithm {
    case object RSA extends UserPublicKeyAlgorithm("rsa")
    case object ECDSA extends UserPublicKeyAlgorithm("ecdsa")

    def fromString(value: String): Option[UserPublicKeyAlgorithm] = value match {
      case RSA.value => Some(RSA)
      case ECDSA.value => Some(ECDSA)
      case _ => None
    }
  }

  sealed abstract class UserPublicKeyType(val value: String)
  object UserPublicKeyType {
    case object PEM extends UserPublicKeyType("pem")
    case object OpenSSH extends UserPublicKeyType("openssh")

    def fromString(value: String): Option[UserPublicKeyType] = value match {
      case PEM.value => Some(PEM)
      case OpenSSH.value => Some(OpenSSH)
      case _ => None
    }
  }

  def findByUserId(userId: String)(implicit session: DBSession): List[UserPublicKey] = {

    withSQL {
      select
        .from(UserPublicKey as upk)
        .where
        .eq(upk.userId, userId)
        .and
        .isNull(upk.deletedAt)
        .orderBy(upk.id.desc)
    }.map(UserPublicKey(upk.resultName)).list.apply()
  }

  def findActiveByUserId(userId: String)(implicit session: DBSession): Option[UserPublicKey] = {

    withSQL {
      select
        .from(UserPublicKey as upk)
        .where
        .eq(upk.userId, userId)
        .and
        .isNull(upk.deletedAt)
        .orderBy(upk.id.desc)
        .limit(1)
    }.map(UserPublicKey(upk.resultName)).single.apply()
  }

  def write(rows: List[UserPublicKey])(session: DBSession): Unit = {
    implicit val s: DBSession = session

    val builder = BatchParamsBuilder {
      rows.map { row =>
        Seq(
          column.userId -> row.userId,
          column.keyValue -> row.keyValue,
          column.keyAlgorithm -> row.keyAlgorithm,
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
