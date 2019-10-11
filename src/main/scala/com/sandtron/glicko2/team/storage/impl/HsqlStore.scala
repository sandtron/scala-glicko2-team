package com.sandtron.glicko2.team.store.impl
import scala.io.Source.fromURL
import scala.util.Using
import scala.util.Success
import scala.util.Failure
import java.sql._
import scala.util.Try
import scala.util.Using.Releasable
import com.sandtron.glicko2.team.storage.impl.BaseSqlStore
import mr.sandtron.glicko2.team.storage.GPlayerStore
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GPlayer

class HsqlStore(val dbName: String, val defaultPlayer: Player) extends BaseSqlStore with GPlayerStore {

  def usingCon[A](withConFun: Connection => A): Try[A] = {
    Class.forName("org.hsqldb.jdbcDriver");
    Using(DriverManager.getConnection(s"jdbc:hsqldb:file:db/$dbName", "SA", ""))(withConFun)
  }

  def updateGPlayer(gPlayer: GPlayer): GPlayer = {
    upsert(
      queries.get("update.gplayer").get,
      gPlayer.stats.rating,
      gPlayer.stats.deviation,
      gPlayer.stats.volatility,
      gPlayer.name
    )
    loadGPlayer(gPlayer.name).get
  }

  def createGPlayer(name: String, player: Player): GPlayer = {
    upsert(
      queries.get("insert.gplayer").get,
      name,
      player.rating.bigDecimal,
      player.deviation.bigDecimal,
      player.volatility.bigDecimal
    )
    loadGPlayer(name).get
  }

  def loadGPlayer(name: String): Option[GPlayer] =
    select(s"${queries.get("select.gplayer.base").get} WHERE NAME = ?", name)(
      _.unsafeHeadOption().map(
        rs =>
          GPlayer(
            rs.getString("NAME"),
            Player(rs.getBigDecimal("RATING"), rs.getBigDecimal("DEVIATION"), rs.getBigDecimal("VOLATILITY"))
          )
      )
    ).toOption.flatten

  def loadGPlayers(): Seq[GPlayer] =
    select(queries.get("select.gplayer.base").get)(
      _.unsafeIterate()
        .map(
          rs =>
            GPlayer(
              rs.getString("NAME"),
              Player(rs.getBigDecimal("RATING"), rs.getBigDecimal("DEVIATION"), rs.getBigDecimal("VOLATILITY"))
            )
        )
        .toStream
    ).get

}
