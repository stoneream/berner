package berner.daemon.message_delete_daemon

import berner.database.{HubMessageDeleteQueueReader, HubMessageDeleteQueueWriter}
import berner.logging.Logger
import cats.effect.IO
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.{JDA, JDABuilder}
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.util.control.Exception.allCatch

object MessageDeleteDamon extends Logger {
  def task(discordBotToken: String): IO[Unit] = {
    (for {
      jda <- preExecute(discordBotToken)
      _ <- execute(jda).handleErrorWith { e =>
        IO {
          logger.error("メッセージ削除処理中にエラーが発生しました。", e)
        }
      }
      _ <- postExecute()
    } yield ()).foreverM
  }

  private def preExecute(discordBotToken: String): IO[JDA] = IO {
    JDABuilder.createDefault(discordBotToken).build().awaitReady()
  }

  private def execute(jda: JDA): IO[Unit] = {
    val deleteTask = IO {
      // 100件ずつ取得して削除
      val rows = DB.localTx { s => HubMessageDeleteQueueReader.pendings(limit = 100)(s) }

      if (rows.isEmpty) {
        // do nothing
      } else {
        logger.info(s"削除対象のメッセージを取得しました。(${rows.size})")

        val groupedRows = rows
          .groupBy { case (_, hmm) => hmm.guildId } // サーバーごとにグルーピング
          .map { case (guildId, rows) => (guildId, rows.groupBy(_._2.hubGuildMessageChannelId)) } // ハブチャンネルごとにグルーピング

        groupedRows.foreach { case (guildId, hub) =>
          hub.foreach { case (hubMessageChannelId, queue) =>
            val messageIds = queue.map(_._2.hubMessageId)
            val queueIds = queue.map(_._1.id)

            allCatch.either {
              for {
                guild <- Option(jda.getGuildById(guildId))
                hubChannel <- Option(guild.getChannelById(classOf[TextChannel], hubMessageChannelId))
              } yield {
                messageIds.foreach { id =>
                  hubChannel.deleteMessageById(id).complete()
                  Thread.sleep(250) // 雑にスリープ...
                }
              }
            } match { // 削除結果に応じてDBのステータスを更新
              case Left(e) =>
                logger.error(s"メッセージの削除中にエラーが発生しました。${messageIds.size}", e)
                val now = OffsetDateTime.now()
                DB.localTx { s =>
                  HubMessageDeleteQueueWriter.markFailedByMessageIds(queueIds, now)(s)
                }
              case Right(_) =>
                logger.info(s"メッセージを削除しました。(${messageIds.size})")
                val now = OffsetDateTime.now()
                DB.localTx { s =>
                  HubMessageDeleteQueueWriter.markDeleteByMessageIds(queueIds, now)(s)
                }
            }
          }
        }
      }
    }
    val waitTask = IO.sleep(15.seconds)

    (deleteTask *> waitTask).foreverM
  }

  private def postExecute(): IO[Unit] = IO {}
}
