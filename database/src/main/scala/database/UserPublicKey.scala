package database

import scalikejdbc._
import java.time.{OffsetDateTime}

case class UserPublicKey(
    id: Long,
    userId: String,
    keyValue: String,
    keyAlgorithm: String,
    keyType: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime] = None
) {

  def save()(implicit session: DBSession): UserPublicKey = UserPublicKey.save(this)(session)

  def destroy()(implicit session: DBSession): Int = UserPublicKey.destroy(this)(session)

}

object UserPublicKey extends SQLSyntaxSupport[UserPublicKey] {

  override val schemaName = Some("berner")

  override val tableName = "user_public_keys"

  override val columns = Seq("id", "user_id", "key_value", "key_algorithm", "key_type", "created_at", "updated_at", "deleted_at")

  def apply(upk: SyntaxProvider[UserPublicKey])(rs: WrappedResultSet): UserPublicKey = autoConstruct(rs, upk)
  def apply(upk: ResultName[UserPublicKey])(rs: WrappedResultSet): UserPublicKey = autoConstruct(rs, upk)

  val upk = UserPublicKey.syntax("upk")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession): Option[UserPublicKey] = {
    withSQL {
      select.from(UserPublicKey as upk).where.eq(upk.id, id)
    }.map(UserPublicKey(upk.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession): List[UserPublicKey] = {
    withSQL(select.from(UserPublicKey as upk)).map(UserPublicKey(upk.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession): Long = {
    withSQL(select(sqls.count).from(UserPublicKey as upk)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession): Option[UserPublicKey] = {
    withSQL {
      select.from(UserPublicKey as upk).where.append(where)
    }.map(UserPublicKey(upk.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession): List[UserPublicKey] = {
    withSQL {
      select.from(UserPublicKey as upk).where.append(where)
    }.map(UserPublicKey(upk.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession): Long = {
    withSQL {
      select(sqls.count).from(UserPublicKey as upk).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(
      userId: String,
      keyValue: String,
      keyAlgorithm: String,
      keyType: String,
      createdAt: OffsetDateTime,
      updatedAt: OffsetDateTime,
      deletedAt: Option[OffsetDateTime] = None
  )(implicit session: DBSession): UserPublicKey = {
    val generatedKey = withSQL {
      insert
        .into(UserPublicKey)
        .namedValues(
          column.userId -> userId,
          column.keyValue -> keyValue,
          column.keyAlgorithm -> keyAlgorithm,
          column.keyType -> keyType,
          column.createdAt -> createdAt,
          column.updatedAt -> updatedAt,
          column.deletedAt -> deletedAt
        )
    }.updateAndReturnGeneratedKey.apply()

    UserPublicKey(
      id = generatedKey,
      userId = userId,
      keyValue = keyValue,
      keyAlgorithm = keyAlgorithm,
      keyType = keyType,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt
    )
  }

  def batchInsert(entities: collection.Seq[UserPublicKey])(implicit session: DBSession): List[Int] = {
    val params: collection.Seq[Seq[(String, Any)]] = entities.map(entity =>
      Seq(
        "userId" -> entity.userId,
        "keyValue" -> entity.keyValue,
        "keyAlgorithm" -> entity.keyAlgorithm,
        "keyType" -> entity.keyType,
        "createdAt" -> entity.createdAt,
        "updatedAt" -> entity.updatedAt,
        "deletedAt" -> entity.deletedAt
      )
    )
    SQL("""insert into user_public_keys(
      user_id,
      key_value,
      key_algorithm,
      key_type,
      created_at,
      updated_at,
      deleted_at
    ) values (
      {userId},
      {keyValue},
      {keyAlgorithm},
      {keyType},
      {createdAt},
      {updatedAt},
      {deletedAt}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: UserPublicKey)(implicit session: DBSession): UserPublicKey = {
    withSQL {
      update(UserPublicKey)
        .set(
          column.id -> entity.id,
          column.userId -> entity.userId,
          column.keyValue -> entity.keyValue,
          column.keyAlgorithm -> entity.keyAlgorithm,
          column.keyType -> entity.keyType,
          column.createdAt -> entity.createdAt,
          column.updatedAt -> entity.updatedAt,
          column.deletedAt -> entity.deletedAt
        )
        .where
        .eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: UserPublicKey)(implicit session: DBSession): Int = {
    withSQL { delete.from(UserPublicKey).where.eq(column.id, entity.id) }.update.apply()
  }

}
