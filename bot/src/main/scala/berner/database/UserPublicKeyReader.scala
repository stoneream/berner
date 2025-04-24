package berner.database

import berner.model.register_key.UserPublicKey
import scalikejdbc._

object UserPublicKeyReader {
  def findByUserId(userId: String)(implicit session: DBSession): List[UserPublicKey] = {
    val upk = UserPublicKey.syntax("upk")
    withSQL {
      select
        .from(UserPublicKey as upk)
        .where
        .eq(upk.userId, userId)
        .and
        .isNull(upk.deletedAt)
        .orderBy(upk.id.desc)
    }.map(UserPublicKey(upk.resultName)).list.apply()
  }

  def findActiveByUserId(userId: String)(implicit session: DBSession): Option[UserPublicKey] = {
    val upk = UserPublicKey.syntax("upk")
    withSQL {
      select
        .from(UserPublicKey as upk)
        .where
        .eq(upk.userId, userId)
        .and
        .isNull(upk.deletedAt)
        .orderBy(upk.id.desc)
        .limit(1)
    }.map(UserPublicKey(upk.resultName)).single.apply()
  }
}
