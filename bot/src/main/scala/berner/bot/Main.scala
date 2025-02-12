package berner.bot

import berner.daemon.hub_daemon.HubDaemon
import berner.daemon.message_delete_daemon.MessageDeleteDamon
import berner.daemon.register_key_daemon.RegisterKeyDaemon
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import scalikejdbc.config.DBs

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val loadConfig = IO {
      val config = ConfigFactory.load()
      config.getString("discord.token")
    }
    val setupDB = IO {
      DBs.setupAll()
    }
    val closeDB = IO {
      DBs.closeAll()
    }

    (for {
      discordBotToken <- loadConfig
      _ <- setupDB
      hubDaemonFiber <- HubDaemon.task(discordBotToken).start
      registerKeyDaemonFiber <- RegisterKeyDaemon.task(discordBotToken).start
      messageDeleteDamonFiber <- MessageDeleteDamon.task(discordBotToken).start
      _ <- hubDaemonFiber.join
      _ <- registerKeyDaemonFiber.join
      _ <- messageDeleteDamonFiber.join
    } yield ()).guarantee(closeDB).as(ExitCode.Success)
  }
}
