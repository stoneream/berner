package berner.database

import berner.model.hub.HubMessageMapping
import scalikejdbc._

object HubMessageMappingSyntax extends SQLSyntaxSupport[HubMessageMapping] {
  override def tableName: String = "hub_message_mappings"

  def apply(hm: ResultName[HubMessageMapping])(rs: WrappedResultSet): HubMessageMapping =
    HubMessageMapping(
      id = rs.long(hm.id),
      guildId = rs.string(hm.guildId),
      sourceGuildMessageChannelId = rs.string(hm.sourceGuildMessageChannelId),
      sourceThreadMessageChannelId = rs.stringOpt(hm.sourceThreadMessageChannelId),
      sourceMessageId = rs.string(hm.sourceMessageId),
      hubGuildMessageChannelId = rs.string(hm.hubGuildMessageChannelId),
      hubMessageId = rs.string(hm.hubMessageId),
      createdAt = rs.offsetDateTime(hm.createdAt),
      updatedAt = rs.offsetDateTime(hm.updatedAt),
      deletedAt = rs.offsetDateTimeOpt(hm.deletedAt)
    )
}
