package berner.database

import berner.model.hub.HubMessage
import scalikejdbc._

object HubMessageReader {

  def findForUpdate(
      sourceMessageId: String,
      sourceChannelId: String,
      guildId: String
  )(session: DBSession): Option[HubMessage] = {
    implicit val s: DBSession = session
    val hm = HubMessageSyntax.syntax("hm")

    withSQL {
      selectFrom(HubMessageSyntax as hm).where
        .eq(hm.sourceMessageId, sourceMessageId)
        .and
        .eq(hm.sourceChannelId, sourceChannelId)
        .and
        .eq(hm.guildId, guildId)
        .and
        .isNull(hm.deletedAt)
        .forUpdate
    }.map(HubMessageSyntax(hm.resultName)).single.apply()
  }

  def find(
      sourceMessageId: String,
      sourceChannelId: String,
      guildId: String
  )(session: DBSession): Option[HubMessage] = {
    implicit val s: DBSession = session
    val hm = HubMessageSyntax.syntax("hm")

    withSQL {
      selectFrom(HubMessageSyntax as hm).where
        .eq(hm.sourceMessageId, sourceMessageId)
        .and
        .eq(hm.sourceChannelId, sourceChannelId)
        .and
        .eq(hm.guildId, guildId)
        .and
        .isNull(hm.deletedAt)
    }.map(HubMessageSyntax(hm.resultName)).single.apply()
  }

}
