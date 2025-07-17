package database

import scalikejdbc._
import java.time.{OffsetDateTime}

case class HubMessages(
  id: Long,
  sourceMessageId: String,
  sourceChannelId: String,
  messageId: String,
  channelId: String,
  guildId: String,
  createdAt: OffsetDateTime,
  updatedAt: OffsetDateTime,
  deletedAt: Option[OffsetDateTime] = None) {

  def save()(implicit session: DBSession): HubMessages = HubMessages.save(this)(session)

  def destroy()(implicit session: DBSession): Int = HubMessages.destroy(this)(session)

}


object HubMessages extends SQLSyntaxSupport[HubMessages] {

  override val schemaName = Some("berner")

  override val tableName = "hub_messages"

  override val columns = Seq("id", "source_message_id", "source_channel_id", "message_id", "channel_id", "guild_id", "created_at", "updated_at", "deleted_at")

  def apply(hm: SyntaxProvider[HubMessages])(rs: WrappedResultSet): HubMessages = autoConstruct(rs, hm)
  def apply(hm: ResultName[HubMessages])(rs: WrappedResultSet): HubMessages = autoConstruct(rs, hm)

  val hm = HubMessages.syntax("hm")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession): Option[HubMessages] = {
    withSQL {
      select.from(HubMessages as hm).where.eq(hm.id, id)
    }.map(HubMessages(hm.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession): List[HubMessages] = {
    withSQL(select.from(HubMessages as hm)).map(HubMessages(hm.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession): Long = {
    withSQL(select(sqls.count).from(HubMessages as hm)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession): Option[HubMessages] = {
    withSQL {
      select.from(HubMessages as hm).where.append(where)
    }.map(HubMessages(hm.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession): List[HubMessages] = {
    withSQL {
      select.from(HubMessages as hm).where.append(where)
    }.map(HubMessages(hm.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession): Long = {
    withSQL {
      select(sqls.count).from(HubMessages as hm).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(
    sourceMessageId: String,
    sourceChannelId: String,
    messageId: String,
    channelId: String,
    guildId: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime] = None)(implicit session: DBSession): HubMessages = {
    val generatedKey = withSQL {
      insert.into(HubMessages).namedValues(
        column.sourceMessageId -> sourceMessageId,
        column.sourceChannelId -> sourceChannelId,
        column.messageId -> messageId,
        column.channelId -> channelId,
        column.guildId -> guildId,
        column.createdAt -> createdAt,
        column.updatedAt -> updatedAt,
        column.deletedAt -> deletedAt
      )
    }.updateAndReturnGeneratedKey.apply()

    HubMessages(
      id = generatedKey,
      sourceMessageId = sourceMessageId,
      sourceChannelId = sourceChannelId,
      messageId = messageId,
      channelId = channelId,
      guildId = guildId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt)
  }

  def batchInsert(entities: collection.Seq[HubMessages])(implicit session: DBSession): List[Int] = {
    val params: collection.Seq[Seq[(String, Any)]] = entities.map(entity =>
      Seq(
        "sourceMessageId" -> entity.sourceMessageId,
        "sourceChannelId" -> entity.sourceChannelId,
        "messageId" -> entity.messageId,
        "channelId" -> entity.channelId,
        "guildId" -> entity.guildId,
        "createdAt" -> entity.createdAt,
        "updatedAt" -> entity.updatedAt,
        "deletedAt" -> entity.deletedAt))
    SQL("""insert into hub_messages(
      source_message_id,
      source_channel_id,
      message_id,
      channel_id,
      guild_id,
      created_at,
      updated_at,
      deleted_at
    ) values (
      {sourceMessageId},
      {sourceChannelId},
      {messageId},
      {channelId},
      {guildId},
      {createdAt},
      {updatedAt},
      {deletedAt}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: HubMessages)(implicit session: DBSession): HubMessages = {
    withSQL {
      update(HubMessages).set(
        column.id -> entity.id,
        column.sourceMessageId -> entity.sourceMessageId,
        column.sourceChannelId -> entity.sourceChannelId,
        column.messageId -> entity.messageId,
        column.channelId -> entity.channelId,
        column.guildId -> entity.guildId,
        column.createdAt -> entity.createdAt,
        column.updatedAt -> entity.updatedAt,
        column.deletedAt -> entity.deletedAt
      ).where.eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: HubMessages)(implicit session: DBSession): Int = {
    withSQL { delete.from(HubMessages).where.eq(column.id, entity.id) }.update.apply()
  }

}
