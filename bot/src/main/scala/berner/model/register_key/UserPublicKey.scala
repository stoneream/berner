package berner.model.register_key

import scalikejdbc._

import java.time.OffsetDateTime

case class UserPublicKey(
    id: Long,
    userId: String,
    keyPem: String,
    keyType: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)

object UserPublicKey extends SQLSyntaxSupport[UserPublicKey] {
  override def tableName: String = "user_public_keys"

  def apply(rn: ResultName[UserPublicKey])(rs: WrappedResultSet): UserPublicKey = autoConstruct(rs, rn)

  sealed abstract class UserPublicKeyType(val value: String)
  object UserPublicKeyType {
    case object RSA extends UserPublicKeyType("rsa")
    case object ECDSA extends UserPublicKeyType("ecdsa")

    def fromString(value: String): Option[UserPublicKeyType] = value match {
      case RSA.value => Some(RSA)
      case ECDSA.value => Some(ECDSA)
      case _ => None
    }
  }
}
