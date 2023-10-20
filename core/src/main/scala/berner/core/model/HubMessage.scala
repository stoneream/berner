package berner.core.model

import java.time.LocalDateTime

case class HubMessage(
    id: Long,
    sourceMessageId: String,
    sourceChannelId: String,
    messageId: String,
    channelId: String,
    guildId: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: Option[LocalDateTime]
)
