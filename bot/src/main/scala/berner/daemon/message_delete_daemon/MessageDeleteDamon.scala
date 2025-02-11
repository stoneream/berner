package berner.daemon.message_delete_daemon

import berner.database.{HubMessageDeleteQueueReader, HubMessageDeleteQueueWriter}
import berner.logging.Logger
import cats.effect.IO
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.control.Exception.allCatch

object MessageDeleteDamon extends Logger {
  def task(n: Int): IO[Unit] = {
    (preExecute() >> execute(???) >> postExecute()).foreverM
  }

  private def preExecute(): IO[Unit] = IO {}

  private def execute(jda: JDA): IO[Unit] = IO {
    val rows = DB.localTx { s => HubMessageDeleteQueueReader.pendings(limit = 100)(s) }

    val groupedRows = rows
      .groupBy { case (_, hmm) => hmm.guildId } // サーバーごとにグルーピング
      .map { case (guildId, rows) => (guildId, rows.groupBy(_._2.hubGuildMessageChannelId)) } // ハブチャンネルごとにグルーピング

    groupedRows.map { case (guildId, hub) =>
      hub.map { case (hubMessageChannelId, queue) =>
        val messageIds = queue.map(_._2.hubMessageId)

        allCatch.either {
          for {
            guild <- Option(jda.getGuildById(guildId))
            hubChannel <- Option(guild.getChannelById(classOf[TextChannel], hubMessageChannelId))
          } yield {
            hubChannel.deleteMessagesByIds(messageIds.asJava)
          }
        } match {
          case Left(e) =>
            error("メッセージの削除中にエラーが発生しました", e)
            val ids = queue.map(_._1.id)
            val now = OffsetDateTime.now()
            DB.localTx { s =>
              HubMessageDeleteQueueWriter.markFailedByMessageIds(ids, now)(s)
            }
          case Right(_) =>
            val ids = queue.map(_._1.id)
            val now = OffsetDateTime.now()
            DB.localTx { s =>
              HubMessageDeleteQueueWriter.markDeleteByMessageIds(ids, now)(s)
            }
        }
      }
    }
  }

  private def postExecute(): IO[Unit] = IO {}
}
