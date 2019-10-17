package com.sandtron.glicko2.team.store.impl
import scala.io.Source.fromURL
import scala.util.Using
import scala.util.Success
import scala.util.Failure
import java.sql._
import scala.util.Try
import scala.util.Using.Releasable
import com.sandtron.glicko2.team.storage.impl.BaseSqlStore
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GPlayer
import mr.sandtron.glicko2.team.storage.Glicko2TeamStore
import java.time.LocalDateTime
import com.sandtron.glicko2.team.Model.GTeamMatch
import com.sandtron.glicko2.team.Model.GTeam
import com.sandtron.glicko2.team.Model.Outcome
import org.slf4j.LoggerFactory
import com.sandtron.glicko2.team.Model.GMatchRecord
import java.{util => ju}
object HsqlStore {
  lazy val logger = LoggerFactory.getLogger(classOf[HsqlStore])

}
class HsqlStore(val dbName: String, val defaultPlayer: Player) extends BaseSqlStore with Glicko2TeamStore {

  import BaseSqlStore._
  import BaseSqlStore.implicits._

  private lazy val logger = HsqlStore.logger

  def usingCon[A](withConFun: Connection => A): Try[A] = {
    Class.forName("org.hsqldb.jdbcDriver");
    Using(
      DriverManager.getConnection(s"jdbc:hsqldb:file:db/$dbName;shutdown=true;hsqldb.reconfig_logging=false", "SA", "")
    )(withConFun)
  }
  private def createGPlayerFromRs(rs: ResultSet): GPlayer = {
    GPlayer(
      rs.getString("NAME"),
      Player(rs.getBigDecimal("RATING"), rs.getBigDecimal("DEVIATION"), rs.getBigDecimal("VOLATILITY"))
    )
  }
  private def createGTeamRecordFromRs(rs: ResultSet): GMatchRecord =
    new GMatchRecord {

      private def loadTeamMembers(teamId: Long): Iterator[String] =
        queries
          .get("select.gteam_member.byTeamId")
          .iterator
          .flatMap(
            select(_, teamId)
              .map(_.getString("NAME"))
          )
      private val t1: Long = rs.getLong("TEAM_1")
      private val t2: Long = rs.getLong("TEAM_2")

      val outcome: Outcome.Outcome = Outcome.withName(rs.getString("OUTCOME"))
      val gameTime: Date           = rs.getDate("CREATED_DATETIME")

      lazy val team: Seq[String]      = loadTeamMembers(t1).toSeq
      lazy val opponents: Seq[String] = loadTeamMembers(t2).toSeq
      lazy val gPlayers: Seq[String]  = team ++ opponents
    }

  // Members declared in mr.sandtron.glicko2.team.storage.GPlayerStore
  def loadGPlayers(): Iterator[GPlayer] =
    queries
      .get("select.gplayer.base")
      .iterator
      .flatMap(select(_).map(createGPlayerFromRs))
  def getGPlayer(name: String): GPlayer =
    queries
      .get("select.gplayer.base")
      .map(sql => s"$sql where name = ?")
      .flatMap(select(_, name).map(createGPlayerFromRs).nextOption())
      .getOrElse(GPlayer(name, defaultPlayer))

  def upsertGPlayer(gplayer: GPlayer) = {
    val existingPlayer = queries
      .get("select.gplayer.base")
      .map(sql => s"$sql where name = ?")
      .flatMap(select(_, gplayer.name).map(createGPlayerFromRs).nextOption())

    (existingPlayer match {
      case Some(_) => queries.get("update.gplayer")
      case None    => queries.get("insert.gplayer")
    }).foreach(
      upsert(_, gplayer.stats.rating, gplayer.stats.deviation, gplayer.stats.volatility, gplayer.time, gplayer.name)
    )
  }

  // Members declared in mr.sandtron.glicko2.team.storage.GTeamMatchStore
  def addMatch(gMatchRecord: GMatchRecord): Unit = {
    def addTeam(gPlayers: Seq[String]): Long =
      queries
        .get("select.gteam.nextId")
        .flatMap(select(_).map(_.getLong(1)).nextOption)
        .map(tid => {
          queries
            .get("insert.gteam_member")
            .foreach(sql => gPlayers.foreach(upsert(sql, tid, _)))
          tid
        })
        .get

    queries
      .get("insert.gteam_match")
      .foreach(
        upsert(
          _,
          addTeam(gMatchRecord.team),
          addTeam(gMatchRecord.opponents),
          gMatchRecord.outcome.toString(),
          gMatchRecord.gameTime
        )
      )
  }
  def loadMatches(): Iterator[GMatchRecord] =
    queries
      .get("select.gteam_match.base")
      .map(sql => s"$sql ORDER BY CREATED_DATETIME ASC")
      .iterator
      .flatMap(select(_).map(createGTeamRecordFromRs))

  def loadMatches(start: ju.Date): Iterator[GMatchRecord] =
    queries
      .get("select.gteam_match.base")
      .map(sql => s"$sql WHERE CREATED_DATETIME > ? ORDER BY CREATED_DATETIME ASC")
      .iterator
      .flatMap(select(_, start).map(createGTeamRecordFromRs))

  // def updateGPlayer(gPlayer: GPlayer): GPlayer = {
  //   upsert(
  //     queries.get("update.gplayer").get,
  //     gPlayer.stats.rating,
  //     gPlayer.stats.deviation,
  //     gPlayer.stats.volatility,
  //     gPlayer.name
  //   )

  //   loadGPlayer(gPlayer.name).get
  // }

  // def createGPlayer(name: String, player: Player): GPlayer = {
  //   upsert(
  //     queries.get("insert.gplayer").get,
  //     name,
  //     player.rating.bigDecimal,
  //     player.deviation.bigDecimal,
  //     player.volatility.bigDecimal
  //   )
  //   loadGPlayer(name).get
  // }

  // def loadGPlayer(name: String): Option[GPlayer] =
  //   queries
  //     .get("select.gplayer.latest")
  //     .iterator
  //     .flatMap(
  //       sql => select(sql, name).map(createGPlayerFromRs)
  //     )
  //     .nextOption()

  // def loadGPlayers(): Iterator[GPlayer] =
  //   select(queries.get("select.gplayer.base").get)
  //     .map(createGPlayerFromRs)

  // private def insertTeamForMatch(team: GTeam): Long =
  //   select(queries.get("select.gteam.nextId").get)
  //     .map(rs => rs.getLong(1))
  //     .map(teamId => {
  //       team.gPlayers
  //         .foreach(
  //           gPlayer =>
  //             queries
  //               .get("insert.gteammember")
  //               .foreach(
  //                 upsert(
  //                   _,
  //                   teamId,
  //                   gPlayer.name,
  //                   gPlayer.stats.rating,
  //                   gPlayer.stats.deviation,
  //                   gPlayer.stats.volatility
  //                 )
  //               )
  //         )
  //       teamId
  //     })
  //     .next

  // def addMatch(gTeamMatch: GTeamMatch): Unit = upsert(
  //   queries.get("insert.gteammatch").get,
  //   insertTeamForMatch(gTeamMatch.team),
  //   insertTeamForMatch(gTeamMatch.opponents),
  //   gTeamMatch.outcome.toString()
  // )
  // private def loadTeam(id: Long) =
  //   new LazyGTeam({
  //     select(queries.get("select.gplayer.byTeamId").get, id)
  //       .map(createGPlayerFromRs)
  //       .toList
  //   })

  // def loadMatches(start: LocalDateTime, end: LocalDateTime, gPlayers: GPlayer*): Iterator[GTeamMatch] = ???
  // def loadMatches(start: LocalDateTime, gPlayers: GPlayer*): Iterator[GTeamMatch]                     = ???
  // def loadMatches(gPlayers: GPlayer*): Iterator[GTeamMatch]                                           = ???
  // def loadMatches(start: LocalDateTime, end: LocalDateTime): Iterator[GTeamMatch] = loadMatches(
  //   queries
  //     .get("select.gteam_match.base")
  //     .map(sql => s"$sql where CREATED_TIME >= ? ORDER BY CREATED_TIME ASC"),
  //   start,
  //   end
  // )

  // def loadMatches(start: LocalDateTime): Iterator[GTeamMatch] =
  //   loadMatches(
  //     queries
  //       .get("select.gteam_match.base")
  //       .map(sql => s"$sql where CREATED_TIME >= ? ORDER BY CREATED_TIME ASC"),
  //     start
  //   )

  // def loadMatches(): Iterator[GTeamMatch] =
  //   loadMatches(queries.get("select.gteam_match.base").map(sql => s"$sql ORDER BY CREATED_TIME ASC"))

  // private def loadMatches(matchSql: Option[String]): Iterator[GTeamMatch] = {
  //   matchSql.iterator
  //     .flatMap(
  //       select(_).map(
  //         rs =>
  //           GTeamMatch(
  //             loadTeam(rs.getLong("TEAM_1")),
  //             loadTeam(rs.getLong("TEAM_2")),
  //             Outcome.withName(rs.getString("OUTCOME"))
  //           )
  //       )
  //     )
  // }
  // private def loadMatches(matchSql: Option[String], params: Any*): Iterator[GTeamMatch] =
  //   matchSql.iterator
  //     .flatMap(
  //       select(_, params: _*).map(
  //         rs =>
  //           GTeamMatch(
  //             loadTeam(rs.getLong("TEAM_1")),
  //             loadTeam(rs.getLong("TEAM_2")),
  //             Outcome.withName(rs.getString("OUTCOME"))
  //           )
  //       )
  //     )
}
