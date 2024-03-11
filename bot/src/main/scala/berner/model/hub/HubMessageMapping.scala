package berner.model.hub

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
