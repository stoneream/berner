package berner.database

import berner.model.hub.HubMessageMapping
import scalikejdbc._

object HubMessageMappingReader {
  private val hmm = HubMessageMapping.syntax("hmm")

  def findForUpdate(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: Option[String],
      sourceMessageId: String,
      guildId: String
  )(session: DBSession): Option[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
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
    }.map(HubMessageMapping(hmm.resultName)).single.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: Option[String],
      sourceMessageId: String,
      guildId: String
  )(session: DBSession): Option[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .eq(hmm.sourceMessageId, sourceMessageId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).single.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      guildId: String
  )(session: DBSession): List[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).list.apply()
  }

  def find(
      sourceGuildMessageChannelId: String,
      sourceThreadMessageChannelId: String,
      guildId: String
  )(session: DBSession): List[HubMessageMapping] = {
    implicit val s: DBSession = session

    withSQL {
      selectFrom(HubMessageMapping as hmm).where
        .eq(hmm.guildId, guildId)
        .and
        .eq(hmm.sourceGuildMessageChannelId, sourceGuildMessageChannelId)
        .and
        .eq(hmm.sourceThreadMessageChannelId, sourceThreadMessageChannelId)
        .and
        .isNull(hmm.deletedAt)
    }.map(HubMessageMapping(hmm.resultName)).list.apply()
  }

}
