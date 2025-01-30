package berner.database

import berner.model.hub.{HubMessageDeleteQueue, HubMessageMapping}
import scalikejdbc._

object HubMessageDeleteQueueReader {
  private val hmdq = HubMessageDeleteQueue.syntax("hmdq")
  private val hmm = HubMessageMapping.syntax("hmm")

  def pendings(limit: Int)(session: DBSession): List[(HubMessageDeleteQueue, HubMessageMapping)] = {
    implicit val s: DBSession = session
    withSQL {
      select(hmdq.*, hmm.*)
        .from(HubMessageDeleteQueue as hmdq)
        .join(HubMessageMapping as hmm)
        .on(sqls.eq(hmdq.hubMessageMappingId, hmm.id).and.isNull(hmm.deletedAt))
        .where
        .eq(hmdq.status, 1)
        .and
        .eq(hmdq.deletedAt, null)
        .limit(limit)
    }.map(rs => (HubMessageDeleteQueue(hmdq.resultName)(rs), HubMessageMapping(hmm.resultName)(rs))).list.apply()
  }
}
