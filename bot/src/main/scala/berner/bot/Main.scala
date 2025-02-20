package berner.bot

import berner.daemon.hub_daemon.HubDaemon
import berner.daemon.message_delete_daemon.MessageDeleteDamon
import berner.model.config.BernerConfig
import cats.effect.{ExitCode, IO, IOApp}
import scalikejdbc.config.DBs

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val loadConfig = IO { BernerConfig.load() }
    val setupDB = IO {
      DBs.setupAll()
    }
    val closeDB = IO {
      DBs.closeAll()
    }

    (for {
      config <- loadConfig
      _ <- setupDB
      hubDaemonFiber <- HubDaemon.task(config).start
      messageDeleteDamonFiber <- MessageDeleteDamon.task(config).start
      _ <- hubDaemonFiber.join
      _ <- messageDeleteDamonFiber.join
    } yield ()).guarantee(closeDB).as(ExitCode.Success)
  }
}
