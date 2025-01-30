package berner.model.hub

import scalikejdbc._

import java.time.OffsetDateTime

case class HubMessageDeleteQueue(
    id: Long,
    hubMessageMappingId: Long,
    status: Int, // 0: pending, 1: success, 2: failed
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    deletedAt: Option[OffsetDateTime]
)

object HubMessageDeleteQueue extends SQLSyntaxSupport[HubMessageDeleteQueue] {
  override def tableName: String = "hub_message_delete_queue"

  def apply(rn: ResultName[HubMessageDeleteQueue])(rs: WrappedResultSet): HubMessageDeleteQueue = autoConstruct(rs, rn)
}
