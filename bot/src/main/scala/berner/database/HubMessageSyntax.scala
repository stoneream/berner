package berner.database

import berner.model.hub.HubMessage
import scalikejdbc._

object HubMessageSyntax extends SQLSyntaxSupport[HubMessage] {
  override def tableName: String = "hub_messages"

  def apply(hm: ResultName[HubMessage])(rs: WrappedResultSet): HubMessage =
    HubMessage(
      id = rs.long(hm.id),
      sourceMessageId = rs.string(hm.sourceMessageId),
      sourceChannelId = rs.string(hm.sourceChannelId),
      messageId = rs.string(hm.messageId),
      channelId = rs.string(hm.channelId),
      guildId = rs.string(hm.guildId),
      createdAt = rs.offsetDateTime(hm.createdAt),
      updatedAt = rs.offsetDateTime(hm.updatedAt),
      deletedAt = rs.offsetDateTimeOpt(hm.deletedAt)
    )
}
