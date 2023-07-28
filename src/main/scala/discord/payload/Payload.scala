package discord.payload

case class Payload[T](op: Int, d: Option[T], s: Option[Int], t: Option[String])

object Payload {
  sealed trait DiscordEvent {
    def value: String
  }

  object DiscordEvent {
    case object Ready extends DiscordEvent {
      override val value = "READY"
    }

    case object Resumed extends DiscordEvent {
      override val value = "RESUMED"
    }

    case object ChannelCreate extends DiscordEvent {
      override val value = "CHANNEL_CREATE"
    }

    case object ChannelUpdate extends DiscordEvent {
      override val value = "CHANNEL_UPDATE"
    }

    case object ChannelDelete extends DiscordEvent {
      override val value = "CHANNEL_DELETE"
    }

    case object ChannelPinsUpdate extends DiscordEvent {
      override val value = "CHANNEL_PINS_UPDATE"
    }

    case object GuildCreate extends DiscordEvent {
      override val value = "GUILD_CREATE"
    }

    case object GuildUpdate extends DiscordEvent {
      override val value = "GUILD_UPDATE"
    }

    case object GuildDelete extends DiscordEvent {
      override val value = "GUILD_DELETE"
    }

    case object GuildBanAdd extends DiscordEvent {
      override val value = "GUILD_BAN_ADD"
    }

    case object GuildBanRemove extends DiscordEvent {
      override val value = "GUILD_BAN_REMOVE"
    }

    case object GuildEmojisUpdate extends DiscordEvent {
      override val value = "GUILD_EMOJIS_UPDATE"
    }

    case object GuildIntegrationsUpdate extends DiscordEvent {
      override val value = "GUILD_INTEGRATIONS_UPDATE"
    }

    case object GuildMemberAdd extends DiscordEvent {
      override val value = "GUILD_MEMBER_ADD"
    }

    case object GuildMemberRemove extends DiscordEvent {
      override val value = "GUILD_MEMBER_REMOVE"
    }

    case object GuildMemberUpdate extends DiscordEvent {
      override val value = "GUILD_MEMBER_UPDATE"
    }

    case object GuildMembersChunk extends DiscordEvent {
      override val value = "GUILD_MEMBERS_CHUNK"
    }

    case object GuildRoleCreate extends DiscordEvent {
      override val value = "GUILD_ROLE_CREATE"
    }

    case object GuildRoleUpdate extends DiscordEvent {
      override val value = "GUILD_ROLE_UPDATE"
    }

    case object GuildRoleDelete extends DiscordEvent {
      override val value = "GUILD_ROLE_DELETE"
    }

    case object MessageCreate extends DiscordEvent {
      override val value = "MESSAGE_CREATE"
    }

    case object MessageUpdate extends DiscordEvent {
      override val value = "MESSAGE_UPDATE"
    }

    case object MessageDelete extends DiscordEvent {
      override val value = "MESSAGE_DELETE"
    }

    case object MessageDeleteBulk extends DiscordEvent {
      override val value = "MESSAGE_DELETE_BULK"
    }

    case object MessageReactionAdd extends DiscordEvent {
      override val value = "MESSAGE_REACTION_ADD"
    }

    case object MessageReactionRemove extends DiscordEvent {
      override val value = "MESSAGE_REACTION_REMOVE"
    }

    case object MessageReactionRemoveAll extends DiscordEvent {
      override val value = "MESSAGE_REACTION_REMOVE_ALL"
    }

    case object PresenceUpdate extends DiscordEvent {
      override val value = "PRESENCE_UPDATE"
    }

    case object TypingStart extends DiscordEvent {
      override val value = "TYPING_START"
    }

    case object UserUpdate extends DiscordEvent {
      override val value = "USER_UPDATE"
    }

    case object VoiceStateUpdate extends DiscordEvent {
      override val value = "VOICE_STATE_UPDATE"
    }

    case object VoiceServerUpdate extends DiscordEvent {
      override val value = "VOICE_SERVER_UPDATE"
    }

    case object WebhooksUpdate extends DiscordEvent {
      override val value = "WEBHOOKS_UPDATE"
    }

    val values: Seq[DiscordEvent] = Seq(
      Ready,
      Resumed,
      ChannelCreate,
      ChannelUpdate,
      ChannelDelete,
      ChannelPinsUpdate,
      GuildCreate,
      GuildUpdate,
      GuildDelete,
      GuildBanAdd,
      GuildBanRemove,
      GuildEmojisUpdate,
      GuildIntegrationsUpdate,
      GuildMemberAdd,
      GuildMemberRemove,
      GuildMemberUpdate,
      GuildMembersChunk,
      GuildRoleCreate,
      GuildRoleUpdate,
      GuildRoleDelete,
      MessageCreate,
      MessageUpdate,
      MessageDelete,
      MessageDeleteBulk,
      MessageReactionAdd,
      MessageReactionRemove,
      MessageReactionRemoveAll,
      PresenceUpdate,
      TypingStart,
      UserUpdate,
      VoiceStateUpdate,
      VoiceServerUpdate,
      WebhooksUpdate
    )

    def fromString(str: String): Option[DiscordEvent] = DiscordEvent.values.find(_.value == str)
  }
}
