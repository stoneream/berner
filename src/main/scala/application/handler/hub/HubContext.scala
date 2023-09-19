package application.handler.hub

import scala.collection.immutable.HashMap

case class HubContext(
    timesChannels: HashMap[String, HubContext.Channel],
    timesThreads: HashMap[String, HubContext.Thread]
)

object HubContext {
  case class Channel(name: String, id: String)

  case class Thread(name: String, id: String, parentId: String)
}
