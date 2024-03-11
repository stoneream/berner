package berner.database

import berner.model.hub.HubMessageMapping
import scalikejdbc._

object HubMessageMappingReader {

  def findForUpdate(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: Option[String],
      sourceMessageId: String,
      guildId: String
  )(session: DBSession): Option[HubMessageMapping] = {
    implicit val s: DBSession = session
    val hmm = HubMessageMappingSyntax.syntax("hmm")

    withSQL {
      selectFrom(HubMessageMappingSyntax as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .eq(hmm.sourceMessageId, sourceMessageId)
        .and
        .isNull(hmm.deletedAt)
        .forUpdate
    }.map(HubMessageMappingSyntax(hmm.resultName)).single.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: Option[String],
      sourceMessageId: String,
      guildId: String
  )(session: DBSession): Option[HubMessageMapping] = {
    implicit val s: DBSession = session
    val hmm = HubMessageMappingSyntax.syntax("hmm")

    withSQL {
      selectFrom(HubMessageMappingSyntax as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .eq(hmm.sourceMessageId, sourceMessageId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMappingSyntax(hmm.resultName)).single.apply()
  }

}
