package database

import scalikejdbc._
import java.time.{OffsetDateTime}

case class HubMessageDeleteQueue(
    id: Long,
    hubMessageMappingId: Long,
    status: Int,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime] = None
) {

  def save()(implicit session: DBSession): HubMessageDeleteQueue = HubMessageDeleteQueue.save(this)(session)

  def destroy()(implicit session: DBSession): Int = HubMessageDeleteQueue.destroy(this)(session)

}

object HubMessageDeleteQueue extends SQLSyntaxSupport[HubMessageDeleteQueue] {

  override val schemaName = Some("berner")

  override val tableName = "hub_message_delete_queues"

  override val columns = Seq("id", "hub_message_mapping_id", "status", "created_at", "updated_at", "deleted_at")

  def apply(hmdq: SyntaxProvider[HubMessageDeleteQueue])(rs: WrappedResultSet): HubMessageDeleteQueue = autoConstruct(rs, hmdq)
  def apply(hmdq: ResultName[HubMessageDeleteQueue])(rs: WrappedResultSet): HubMessageDeleteQueue = autoConstruct(rs, hmdq)

  val hmdq = HubMessageDeleteQueue.syntax("hmdq")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession): Option[HubMessageDeleteQueue] = {
    withSQL {
      select.from(HubMessageDeleteQueue as hmdq).where.eq(hmdq.id, id)
    }.map(HubMessageDeleteQueue(hmdq.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession): List[HubMessageDeleteQueue] = {
    withSQL(select.from(HubMessageDeleteQueue as hmdq)).map(HubMessageDeleteQueue(hmdq.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession): Long = {
    withSQL(select(sqls.count).from(HubMessageDeleteQueue as hmdq)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession): Option[HubMessageDeleteQueue] = {
    withSQL {
      select.from(HubMessageDeleteQueue as hmdq).where.append(where)
    }.map(HubMessageDeleteQueue(hmdq.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession): List[HubMessageDeleteQueue] = {
    withSQL {
      select.from(HubMessageDeleteQueue as hmdq).where.append(where)
    }.map(HubMessageDeleteQueue(hmdq.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession): Long = {
    withSQL {
      select(sqls.count).from(HubMessageDeleteQueue as hmdq).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(hubMessageMappingId: Long, status: Int, createdAt: OffsetDateTime, updatedAt: OffsetDateTime, deletedAt: Option[OffsetDateTime] = None)(implicit
      session: DBSession
  ): HubMessageDeleteQueue = {
    val generatedKey = withSQL {
      insert
        .into(HubMessageDeleteQueue)
        .namedValues(
          column.hubMessageMappingId -> hubMessageMappingId,
          column.status -> status,
          column.createdAt -> createdAt,
          column.updatedAt -> updatedAt,
          column.deletedAt -> deletedAt
        )
    }.updateAndReturnGeneratedKey.apply()

    HubMessageDeleteQueue(
      id = generatedKey,
      hubMessageMappingId = hubMessageMappingId,
      status = status,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt
    )
  }

  def batchInsert(entities: collection.Seq[HubMessageDeleteQueue])(implicit session: DBSession): List[Int] = {
    val params: collection.Seq[Seq[(String, Any)]] = entities.map(entity =>
      Seq(
        "hubMessageMappingId" -> entity.hubMessageMappingId,
        "status" -> entity.status,
        "createdAt" -> entity.createdAt,
        "updatedAt" -> entity.updatedAt,
        "deletedAt" -> entity.deletedAt
      )
    )
    SQL("""insert into hub_message_delete_queues(
      hub_message_mapping_id,
      status,
      created_at,
      updated_at,
      deleted_at
    ) values (
      {hubMessageMappingId},
      {status},
      {createdAt},
      {updatedAt},
      {deletedAt}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: HubMessageDeleteQueue)(implicit session: DBSession): HubMessageDeleteQueue = {
    withSQL {
      update(HubMessageDeleteQueue)
        .set(
          column.id -> entity.id,
          column.hubMessageMappingId -> entity.hubMessageMappingId,
          column.status -> entity.status,
          column.createdAt -> entity.createdAt,
          column.updatedAt -> entity.updatedAt,
          column.deletedAt -> entity.deletedAt
        )
        .where
        .eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: HubMessageDeleteQueue)(implicit session: DBSession): Int = {
    withSQL { delete.from(HubMessageDeleteQueue).where.eq(column.id, entity.id) }.update.apply()
  }

}
