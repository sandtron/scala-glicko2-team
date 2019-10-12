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
import mr.sandtron.glicko2.team.storage.GTeamMatchStore
import java.time.LocalDateTime
import com.sandtron.glicko2.team.Model.GTeamMatch
import com.sandtron.glicko2.team.Model.GTeam
import com.sandtron.glicko2.team.Model.Outcome

class HsqlStore(val dbName: String, val defaultPlayer: Player) extends BaseSqlStore with GPlayerStore with GTeamMatchStore {

  def usingCon[A](withConFun: Connection => A): Try[A] = {
    Class.forName("org.hsqldb.jdbcDriver");
    Using(DriverManager.getConnection(s"jdbc:hsqldb:file:db/$dbName;shutdown=true", "SA", ""))(withConFun)
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

  private def insertTeamForMatch(team: GTeam): Long =
    select(queries.get("select.gteam.nextId").get)(_.unsafeHeadOption().map(rs => rs.getLong(1)).get)
      .map(teamId => {
        team.gPlayers.map(_.name).foreach(upsert(queries.get("insert.gteammember").get, teamId, _));
        teamId
      })
      .get

  def addMatch(gTeamMatch: GTeamMatch): Unit = upsert(
    queries.get("insert.gteammatch").get,
    insertTeamForMatch(gTeamMatch.team),
    insertTeamForMatch(gTeamMatch.opponents),
    gTeamMatch.outcome.toString()
  )
  private def loadTeam(id: Long) =
    GTeam(
      select(queries.get("select.gteammember").get, id)(
        _.unsafeIterate().map(rs => rs.getString(1)).flatMap(loadGPlayer)
      ).get.toList
    )
  def toGTeamMatch(t1: Long, t2: Long, outcome: String): GTeamMatch =
    GTeamMatch(loadTeam(t1), loadTeam(t2), Outcome.withName(outcome))

  def loadMatches(start: LocalDateTime, end: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch] = ???
  def loadMatches(start: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch]                     = ???
  def loadMatches(gPlayers: GPlayer*): Seq[GTeamMatch]                                           = ???
  def loadMatches(start: LocalDateTime, end: LocalDateTime): Seq[GTeamMatch]                     = ???
  def loadMatches(start: LocalDateTime): Seq[GTeamMatch]                                         = ???
  def loadMatches(): Seq[GTeamMatch] = {
    select(queries.get("select.gteam_match.base").get)(
      _.unsafeIterate()
        .map(rs => toGTeamMatch(rs.getLong("TEAM_1"), rs.getLong("TEAM_2"), rs.getString("OUTCOME")))
        .toSeq
    ).get
  }
}
