package berner.model.hub

import java.time.OffsetDateTime

case class HubMessage(
    id: Long,
    sourceMessageId: String,
    sourceChannelId: String,
    messageId: String,
    channelId: String,
    guildId: String,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)
