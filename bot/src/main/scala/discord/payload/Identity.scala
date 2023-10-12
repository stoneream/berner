package discord.payload

case class Identity(token: String, intents: Int, properties: Map[String, String])

object Identity {
  def apply(token: String, intents: Seq[Intent]): Identity =
    Identity(
      token,
      Intent.sum(intents),
      Map.empty
    )

  // https://discord.com/developers/docs/topics/gateway#list-of-intents
  sealed abstract class Intent {
    def value: Int
  }

  object Intent {
    case object GUILDS extends Intent {
      val value: Int = 1 << 0
    }

    case object GUILD_MEMBERS extends Intent {
      val value: Int = 1 << 1
    }

    case object GUILD_BANS extends Intent {
      val value: Int = 1 << 2
    }

    case object GUILD_EMOJIS extends Intent {
      val value: Int = 1 << 3
    }

    case object GUILD_INTEGRATIONS extends Intent {
      val value: Int = 1 << 4
    }

    case object GUILD_WEBHOOKS extends Intent {
      val value: Int = 1 << 5
    }

    case object GUILD_INVITES extends Intent {
      val value: Int = 1 << 6
    }

    case object GUILD_VOICE_STATES extends Intent {
      val value: Int = 1 << 7
    }

    case object GUILD_PRESENCES extends Intent {
      val value: Int = 1 << 8
    }

    case object GUILD_MESSAGES extends Intent {
      val value: Int = 1 << 9
    }

    case object GUILD_MESSAGE_REACTIONS extends Intent {
      val value: Int = 1 << 10
    }

    case object GUILD_MESSAGE_TYPING extends Intent {
      val value: Int = 1 << 11
    }

    case object DIRECT_MESSAGES extends Intent {
      val value: Int = 1 << 12
    }

    case object DIRECT_MESSAGE_REACTIONS extends Intent {
      val value: Int = 1 << 13
    }

    case object DIRECT_MESSAGE_TYPING extends Intent {
      val value: Int = 1 << 14
    }

    case object MESSAGE_CONTENT extends Intent {
      val value: Int = 1 << 15
    }

    def sum(intents: Seq[Intent]): Int = intents.map(_.value).sum
  }
}
