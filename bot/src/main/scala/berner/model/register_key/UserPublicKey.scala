package berner.model.register_key

import scalikejdbc._

import java.time.OffsetDateTime

case class UserPublicKey(
    id: Long,
    userId: String,
    keyValue: String,
    keyType: String,
    keyAlgorithm: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)

object UserPublicKey extends SQLSyntaxSupport[UserPublicKey] {
  override def tableName: String = "user_public_keys"

  def apply(rn: ResultName[UserPublicKey])(rs: WrappedResultSet): UserPublicKey = autoConstruct(rs, rn)

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

}
