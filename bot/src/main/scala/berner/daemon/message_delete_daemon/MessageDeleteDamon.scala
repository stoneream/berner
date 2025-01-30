package berner.daemon.message_delete_daemon

import berner.database.HubMessageDeleteQueueReader
import cats.effect.IO
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import scalikejdbc.DB

import scala.jdk.CollectionConverters.SeqHasAsJava

object MessageDeleteDamon {
  def task(n: Int): IO[Unit] = {
    (preExecute() >> execute(???) >> postExecute()).foreverM
  }

  private def preExecute(): IO[Unit] = IO {}

  private def execute(jda: JDA): IO[Unit] = IO {
    val rows = DB.localTx { s => HubMessageDeleteQueueReader.pendings(100)(s) }
    val groupedRows = rows
      .groupBy { case (hmdq, hmm) => hmm.guildId } // サーバーごとにグルーピング
      .map { case (guildId, rows) => (guildId, rows.groupBy(_._2.hubGuildMessageChannelId)) } // ハブチャンネルごとにグルーピング

    // todo 例外発生時を考慮する
    groupedRows.map { case (guildId, hub) =>
      hub.map { case (hubMessageChannelId, queue) =>
        val messageIds = queue.map(_._2.hubMessageId)
        for {
          guild <- Option(jda.getGuildById(guildId))
          hubChannel <- Option(guild.getChannelById(classOf[TextChannel], hubMessageChannelId))
        } yield {
          hubChannel.deleteMessagesByIds(messageIds.asJava)
        }
      }
    }
  }

  private def postExecute(): IO[Unit] = IO {}
}
