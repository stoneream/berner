package discord

object GatewayOpCode {
  // https://discord.com/developers/docs/topics/opcodes-and-status-codes#gateway

  final val Dispatch = 0
  final val Heartbeat = 1
  final val Identify = 2
  final val PresenceUpdate = 3
  final val VoiceStateUpdate = 4
  final val Resume = 6
  final val Reconnect = 7
  final val RequestGuildMembers = 8
  final val InvalidSession = 9
  final val Hello = 10
  final val HeartbeatAck = 11
}
