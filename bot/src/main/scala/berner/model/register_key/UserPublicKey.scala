package berner.model.register_key

import scalikejdbc._

import java.time.OffsetDateTime

case class UserPublicKey(
    id: Long,
    userId: String,
    key_pem: String,
    key_type: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)

object UserPublicKey extends SQLSyntaxSupport[UserPublicKey] {
  override def tableName: String = "user_public_keys"

  def apply(rn: ResultName[UserPublicKey])(rs: WrappedResultSet): UserPublicKey = autoConstruct(rs, rn)
}
