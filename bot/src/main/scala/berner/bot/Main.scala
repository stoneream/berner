package berner.bot

import berner.daemon.hub_daemon.HubDaemon
import berner.daemon.message_delete_daemon.MessageDeleteDamon
import berner.daemon.register_key_daemon.RegisterKeyDaemon
import berner.logging.Logger
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.config.ConfigFactory
import scalikejdbc.config.DBs

object Main extends IOApp with Logger {
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
      _ <- hubDaemonFiber.join.guarantee {
        IO {
          logger.info("Shutting down hub daemon...")
        }
      }
      _ <- registerKeyDaemonFiber.join.guarantee {
        IO {
          logger.info("Shutting down register key daemon...")
        }
      }
      _ <- messageDeleteDamonFiber.join.guarantee {
        IO {
          logger.info("Shutting down delete daemons...")
        }
      }
    } yield ()).guarantee(closeDB).as(ExitCode.Success)
  }
}
