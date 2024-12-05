package berner.model.hub

import scalikejdbc._

import java.time.OffsetDateTime

case class HubMessageMapping(
    id: Long,
    guildId: String,
    sourceGuildMessageChannelId: String,
    sourceThreadMessageChannelId: Option[String],
    sourceMessageId: String,
    hubGuildMessageChannelId: String,
    hubMessageId: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)

object HubMessageMapping extends SQLSyntaxSupport[HubMessageMapping] {
  override def tableName: String = "hub_message_mappings"

  def apply(rn: ResultName[HubMessageMapping])(rs: WrappedResultSet): HubMessageMapping = autoConstruct(rs, rn)
}
