package database

import scalikejdbc._
import java.time.{OffsetDateTime}

case class HubMessageMappings(
  id: Long,
  guildId: String,
  sourceGuildMessageChannelId: String,
  sourceThreadMessageChannelId: Option[String] = None,
  sourceMessageId: String,
  hubGuildMessageChannelId: String,
  hubMessageId: String,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  deletedAt: Option[OffsetDateTime] = None) {

  def save()(implicit session: DBSession): HubMessageMappings = HubMessageMappings.save(this)(session)

  def destroy()(implicit session: DBSession): Int = HubMessageMappings.destroy(this)(session)

}


object HubMessageMappings extends SQLSyntaxSupport[HubMessageMappings] {

  override val schemaName = Some("berner")

  override val tableName = "hub_message_mappings"

  override val columns = Seq("id", "guild_id", "source_guild_message_channel_id", "source_thread_message_channel_id", "source_message_id", "hub_guild_message_channel_id", "hub_message_id", "created_at", "updated_at", "deleted_at")

  def apply(hmm: SyntaxProvider[HubMessageMappings])(rs: WrappedResultSet): HubMessageMappings = autoConstruct(rs, hmm)
  def apply(hmm: ResultName[HubMessageMappings])(rs: WrappedResultSet): HubMessageMappings = autoConstruct(rs, hmm)

  val hmm = HubMessageMappings.syntax("hmm")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession): Option[HubMessageMappings] = {
    withSQL {
      select.from(HubMessageMappings as hmm).where.eq(hmm.id, id)
    }.map(HubMessageMappings(hmm.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession): List[HubMessageMappings] = {
    withSQL(select.from(HubMessageMappings as hmm)).map(HubMessageMappings(hmm.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession): Long = {
    withSQL(select(sqls.count).from(HubMessageMappings as hmm)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession): Option[HubMessageMappings] = {
    withSQL {
      select.from(HubMessageMappings as hmm).where.append(where)
    }.map(HubMessageMappings(hmm.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession): List[HubMessageMappings] = {
    withSQL {
      select.from(HubMessageMappings as hmm).where.append(where)
    }.map(HubMessageMappings(hmm.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession): Long = {
    withSQL {
      select(sqls.count).from(HubMessageMappings as hmm).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(
    guildId: String,
    sourceGuildMessageChannelId: String,
    sourceThreadMessageChannelId: Option[String] = None,
    sourceMessageId: String,
    hubGuildMessageChannelId: String,
    hubMessageId: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime] = None)(implicit session: DBSession): HubMessageMappings = {
    val generatedKey = withSQL {
      insert.into(HubMessageMappings).namedValues(
        column.guildId -> guildId,
        column.sourceGuildMessageChannelId -> sourceGuildMessageChannelId,
        column.sourceThreadMessageChannelId -> sourceThreadMessageChannelId,
        column.sourceMessageId -> sourceMessageId,
        column.hubGuildMessageChannelId -> hubGuildMessageChannelId,
        column.hubMessageId -> hubMessageId,
        column.createdAt -> createdAt,
        column.updatedAt -> updatedAt,
        column.deletedAt -> deletedAt
      )
    }.updateAndReturnGeneratedKey.apply()

    HubMessageMappings(
      id = generatedKey,
      guildId = guildId,
      sourceGuildMessageChannelId = sourceGuildMessageChannelId,
      sourceThreadMessageChannelId = sourceThreadMessageChannelId,
      sourceMessageId = sourceMessageId,
      hubGuildMessageChannelId = hubGuildMessageChannelId,
      hubMessageId = hubMessageId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt)
  }

  def batchInsert(entities: collection.Seq[HubMessageMappings])(implicit session: DBSession): List[Int] = {
    val params: collection.Seq[Seq[(String, Any)]] = entities.map(entity =>
      Seq(
        "guildId" -> entity.guildId,
        "sourceGuildMessageChannelId" -> entity.sourceGuildMessageChannelId,
        "sourceThreadMessageChannelId" -> entity.sourceThreadMessageChannelId,
        "sourceMessageId" -> entity.sourceMessageId,
        "hubGuildMessageChannelId" -> entity.hubGuildMessageChannelId,
        "hubMessageId" -> entity.hubMessageId,
        "createdAt" -> entity.createdAt,
        "updatedAt" -> entity.updatedAt,
        "deletedAt" -> entity.deletedAt))
    SQL("""insert into hub_message_mappings(
      guild_id,
      source_guild_message_channel_id,
      source_thread_message_channel_id,
      source_message_id,
      hub_guild_message_channel_id,
      hub_message_id,
      created_at,
      updated_at,
      deleted_at
    ) values (
      {guildId},
      {sourceGuildMessageChannelId},
      {sourceThreadMessageChannelId},
      {sourceMessageId},
      {hubGuildMessageChannelId},
      {hubMessageId},
      {createdAt},
      {updatedAt},
      {deletedAt}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: HubMessageMappings)(implicit session: DBSession): HubMessageMappings = {
    withSQL {
      update(HubMessageMappings).set(
        column.id -> entity.id,
        column.guildId -> entity.guildId,
        column.sourceGuildMessageChannelId -> entity.sourceGuildMessageChannelId,
        column.sourceThreadMessageChannelId -> entity.sourceThreadMessageChannelId,
        column.sourceMessageId -> entity.sourceMessageId,
        column.hubGuildMessageChannelId -> entity.hubGuildMessageChannelId,
        column.hubMessageId -> entity.hubMessageId,
        column.createdAt -> entity.createdAt,
        column.updatedAt -> entity.updatedAt,
        column.deletedAt -> entity.deletedAt
      ).where.eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: HubMessageMappings)(implicit session: DBSession): Int = {
    withSQL { delete.from(HubMessageMappings).where.eq(column.id, entity.id) }.update.apply()
  }

}
