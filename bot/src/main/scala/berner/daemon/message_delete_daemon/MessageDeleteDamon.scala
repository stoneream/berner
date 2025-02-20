package berner.daemon.message_delete_daemon

import berner.database.reader.HubMessageDeleteQueueReader
import berner.database.writer.HubMessageDeleteQueueWriter
import berner.logging.Logger
import berner.model.config.BernerConfig
import cats.effect.IO
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.{JDA, JDABuilder}
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.concurrent.duration.DurationInt
import scala.util.control.Exception.allCatch

object MessageDeleteDamon extends Logger {
  def task(config: BernerConfig): IO[Unit] = {
    (for {
      jda <- preExecute(config.discord.token)
      _ <- execute(jda)
      _ <- postExecute()
    } yield ()).foreverM
  }

  private def preExecute(discordBotToken: String): IO[JDA] = IO {
    JDABuilder
      .createDefault(discordBotToken)
      .build()
  }

  private def execute(jda: JDA): IO[Unit] = {
    (IO {
      // 100件ずつ取得して削除
      val rows = DB.localTx { s => HubMessageDeleteQueueReader.pendings(limit = 100)(s) }

      if (rows.isEmpty) {
        // do nothing
      } else {
        logger.info(
          s"削除対象のメッセージを取得しました。({})",
          kv("count", rows.size)
        )

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
                logger.error("メッセージの削除中にエラーが発生しました", e)
                val now = OffsetDateTime.now()
                DB.localTx { s =>
                  HubMessageDeleteQueueWriter.markFailedByMessageIds(queueIds, now)(s)
                }
              case Right(_) =>
                val now = OffsetDateTime.now()
                DB.localTx { s =>
                  HubMessageDeleteQueueWriter.markDeleteByMessageIds(queueIds, now)(s)
                }
            }
          }
        }
      }
    } *> IO.sleep(5.second)).foreverM
  }

  private def postExecute(): IO[Unit] = IO {}
}
