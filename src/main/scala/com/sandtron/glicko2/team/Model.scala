package com.sandtron.glicko2.team
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.github.andriykuba.scala.glicko2.scala.Glicko2

object Model {

  /**
    * A idendifiable player.
    * For team applications, the identifier is used to ensure calculations are tracked.
    */
  case class GPlayer(
      name: String,
      stats: Player
  ) {
    def update(p: Player): GPlayer = GPlayer(name, p)
    override def toString(): String =
      s"$name(${stats.rating},${stats.deviation},${stats.volatility})"

  }

  /**
    * A team for calculation
    */
  case class GTeam(val gPlayers: Seq[GPlayer]) {
    override def toString(): String =
      s"TEAM[${gPlayers.mkString(", ")}]"
  }

  /**
    * The match outcome enumeration
    */
  object Outcome extends Enumeration {
    type Outcome = Value
    val WIN, DRAW, LOSS = Value
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
