package application.handler.hub

import scala.collection.immutable.HashMap

case class HubContext(
    timesChannels: Map[String, HubContext.Channel],
    timesThreads: Map[String, HubContext.Thread]
)

object HubContext {
  case class Channel(name: String, id: String)

  case class Thread(name: String, id: String, parentId: String)

  def empty: HubContext = HubContext(
    timesChannels = HashMap.empty,
    timesThreads = HashMap.empty
  )
}
