package berner.daemon.message_delete_daemon

import berner.database.{HubMessageDeleteQueueReader, HubMessageDeleteQueueWriter}
import berner.logging.Logger
import cats.effect.IO
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.{GuildChannel, GuildMessageChannel, MessageChannel}
import net.dv8tion.jda.api.{JDA, JDABuilder}
import scalikejdbc.DB

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.control.Exception.allCatch

object MessageDeleteDamon extends Logger {
  def task(discordBotToken: String): IO[Unit] = {
    (for {
      jda <- preExecute(discordBotToken)
      _ <- execute(jda).foreverM // クライアントを何度も初期化するのが無駄なのでforeverMでループ
      _ <- postExecute()
    } yield ()).foreverM
  }

  private def preExecute(discordBotToken: String): IO[JDA] = IO {
    JDABuilder
      .createDefault(discordBotToken)
      .build()
  }

  private def execute(jda: JDA): IO[Unit] = {
    IO {
      // 100件ずつ取得して削除
      val rows = DB.localTx { s => HubMessageDeleteQueueReader.pendings(limit = 100)(s) }

      if (rows.isEmpty) {
        // do nothing
      } else {
        info(s"削除対象のメッセージを取得しました。(${rows.size})")

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
                messageIds match {
                  case id :: Nil =>
                    hubChannel.deleteMessageById(id).complete()
                  case ids =>
                    // IllegalArgumentException – If the size of the list less than 2 or more than 100
                    hubChannel.deleteMessagesByIds(ids.asJava).complete()
                }
              }
            } match { // 削除結果に応じてDBのステータスを更新
              case Left(e) =>
                error("メッセージの削除中にエラーが発生しました", e)
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
    }.guarantee(IO {
      val client = jda.getHttpClient
      client.connectionPool.evictAll()
      client.dispatcher.executorService.shutdown()
    })
  }

  private def postExecute(): IO[Unit] = IO {}
}
