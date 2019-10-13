package com.sandtron.glicko2.team
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.github.andriykuba.scala.glicko2.scala.Glicko2
import java.time.LocalDateTime
import java.{util => ju}

object Model {

  /**
    * The match outcome enumeration
    */
  object Outcome extends Enumeration {
    type Outcome = Value
    val WIN, DRAW, LOSS = Value
  }

  trait GMatchRecord {
    def team: Seq[String]
    def opponents: Seq[String]
    def outcome: Outcome.Outcome
    def gameTime: ju.Date
    def toGTeamMatch(gPlayers: Seq[GPlayer]): GTeamMatch =
      toGTeamMatch(gPlayers.map(gp => gp.name -> gp).toMap)
    def toGTeamMatch(gPlayers: Map[String, GPlayer]): GTeamMatch =
      GTeamMatch(GTeam(team.map(gPlayers.get(_).get)), GTeam(opponents.map(gPlayers.get(_).get)), outcome)
  }
  object GMatchRecord {
    def apply(
        initTeam: => Seq[String],
        initOpp: => Seq[String],
        initOutcome: => Outcome.Outcome,
        initGameTime: ju.Date
    ): GMatchRecord = new GMatchRecord {
      lazy val team: Seq[String]        = initTeam
      lazy val opponents: Seq[String]   = initOpp
      lazy val outcome: Outcome.Outcome = initOutcome
      lazy val gameTime: ju.Date        = initGameTime
    }
  }   

  /**
    * A idendifiable player.
    * For team applications, the identifier is used to ensure calculations are tracked.
    */
  trait GPlayer {
    def name: String
    def stats: Player
    def time: ju.Date
    def update(p: Player): GPlayer = GPlayer(name, p)
    override def toString(): String =
      s"$name(${stats.rating},${stats.deviation},${stats.volatility})"

  }
  object GPlayer {
    def apply(initName: => String, initStats: => Player): GPlayer = apply(initName, initStats, new ju.Date())

    def apply(initName: => String, initStats: => Player, initTime: ju.Date): GPlayer = new GPlayer {
      lazy val name  = initName
      lazy val stats = initStats
      val time       = initTime
    }
  }

  /**
    * A team for calculation
    */
  trait GTeam {

    def gPlayers: Seq[GPlayer]
    override def toString(): String =
      s"TEAM[${gPlayers.mkString(", ")}]"
  }
  object GTeam {
    def apply(initGPlayers: => Seq[GPlayer]) = new GTeam() {
      lazy val gPlayers: Seq[com.sandtron.glicko2.team.Model.GPlayer] = initGPlayers
    }
  }

  /**
    * A match between two teams.
    * The outcome refers to the team
    *
    * eg:
    *   GTeamMatch(t1,t2,WIN) => t1 WINS AGAINST t2
    *   GTeamMatch(t1,t2,LOSS) => t1 LOSES AGAINST t2
    *   GTeamMatch(t1,t2,DRAW) => t1 TIES AGAINST t2
    *
    */
  case class GTeamMatch(
      team: GTeam,
      opponents: GTeam,
      outcome: Outcome.Outcome
  ) {

    /**
      * inverts the match
      * eg t1 WINS AGAINST t2 => t2 LOSES AGAINST t1
      */
    def inverse: GTeamMatch =
      GTeamMatch(opponents, team, outcome match {
        case Outcome.WIN  => Outcome.LOSS
        case Outcome.LOSS => Outcome.WIN
        case d            => d
      })
    override def toString: String =
      s"$team ${outcome match {
        case Outcome.WIN  => "WINS";
        case Outcome.DRAW => "TIES";
        case Outcome.LOSS => "LOSES"
      }} AGAINST $opponents"
  }

  case class GMatchUpdate(team: GTeam, opponent: GTeam) {
    def gPlayers: Seq[GPlayer]      = team.gPlayers ++ opponent.gPlayers
    override def toString(): String = s"GMatchUpdate[${gPlayers.mkString(",")}]"
  }

}
