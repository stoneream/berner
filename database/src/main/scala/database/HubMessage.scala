package database

import scalikejdbc._
import java.time.{OffsetDateTime}

case class HubMessage(
    id: Long,
    sourceMessageId: String,
    sourceChannelId: String,
    messageId: String,
    channelId: String,
    guildId: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime] = None
) {

  def save()(implicit session: DBSession): HubMessage = HubMessage.save(this)(session)

  def destroy()(implicit session: DBSession): Int = HubMessage.destroy(this)(session)

}

object HubMessage extends SQLSyntaxSupport[HubMessage] {

  override val schemaName = Some("berner")

  override val tableName = "hub_messages"

  override val columns = Seq("id", "source_message_id", "source_channel_id", "message_id", "channel_id", "guild_id", "created_at", "updated_at", "deleted_at")

  def apply(hm: SyntaxProvider[HubMessage])(rs: WrappedResultSet): HubMessage = autoConstruct(rs, hm)
  def apply(hm: ResultName[HubMessage])(rs: WrappedResultSet): HubMessage = autoConstruct(rs, hm)

  val hm = HubMessage.syntax("hm")

  override val autoSession = AutoSession

  def find(id: Long)(implicit session: DBSession): Option[HubMessage] = {
    withSQL {
      select.from(HubMessage as hm).where.eq(hm.id, id)
    }.map(HubMessage(hm.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession): List[HubMessage] = {
    withSQL(select.from(HubMessage as hm)).map(HubMessage(hm.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession): Long = {
    withSQL(select(sqls.count).from(HubMessage as hm)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession): Option[HubMessage] = {
    withSQL {
      select.from(HubMessage as hm).where.append(where)
    }.map(HubMessage(hm.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession): List[HubMessage] = {
    withSQL {
      select.from(HubMessage as hm).where.append(where)
    }.map(HubMessage(hm.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession): Long = {
    withSQL {
      select(sqls.count).from(HubMessage as hm).where.append(where)
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
      deletedAt: Option[OffsetDateTime] = None
  )(implicit session: DBSession): HubMessage = {
    val generatedKey = withSQL {
      insert
        .into(HubMessage)
        .namedValues(
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

    HubMessage(
      id = generatedKey,
      sourceMessageId = sourceMessageId,
      sourceChannelId = sourceChannelId,
      messageId = messageId,
      channelId = channelId,
      guildId = guildId,
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt
    )
  }

  def batchInsert(entities: collection.Seq[HubMessage])(implicit session: DBSession): List[Int] = {
    val params: collection.Seq[Seq[(String, Any)]] = entities.map(entity =>
      Seq(
        "sourceMessageId" -> entity.sourceMessageId,
        "sourceChannelId" -> entity.sourceChannelId,
        "messageId" -> entity.messageId,
        "channelId" -> entity.channelId,
        "guildId" -> entity.guildId,
        "createdAt" -> entity.createdAt,
        "updatedAt" -> entity.updatedAt,
        "deletedAt" -> entity.deletedAt
      )
    )
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

  def save(entity: HubMessage)(implicit session: DBSession): HubMessage = {
    withSQL {
      update(HubMessage)
        .set(
          column.id -> entity.id,
          column.sourceMessageId -> entity.sourceMessageId,
          column.sourceChannelId -> entity.sourceChannelId,
          column.messageId -> entity.messageId,
          column.channelId -> entity.channelId,
          column.guildId -> entity.guildId,
          column.createdAt -> entity.createdAt,
          column.updatedAt -> entity.updatedAt,
          column.deletedAt -> entity.deletedAt
        )
        .where
        .eq(column.id, entity.id)
    }.update.apply()
    entity
  }

  def destroy(entity: HubMessage)(implicit session: DBSession): Int = {
    withSQL { delete.from(HubMessage).where.eq(column.id, entity.id) }.update.apply()
  }

}
