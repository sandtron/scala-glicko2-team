package com.sandtron.glicko2.team
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Win
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Loss
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Draw
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Game
import com.github.andriykuba.scala.glicko2.scala.Glicko2
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player

object Glicko2Team {
  import Model._
  import Model.Outcome._

  private def gameResultType(outcome: Outcome.Outcome): (GPlayer) => Game =
    outcome match {
      case WIN  => gp => Win(gp.stats)
      case LOSS => gp => Loss(gp.stats)
      case DRAW => gp => Draw(gp.stats)
    }

  /**
    * The general approach is to evaluate a single player against the opposing team.
    * Therefore, this function accepts a GTeamMatch and another function that evaluates a player against a team.
    * This function will then evaluate all players against their opposite team and produce the GMatchUpdate.
    */
  private def evaluateMatch(
      gTeamMatch: GTeamMatch,
      playerVsTeam: (GPlayer, GTeam, Outcome) => GPlayer
  ): GMatchUpdate = {
    def teamVsTeam(
        m: GTeamMatch,
        playerVsTeam: (GPlayer, GTeam, Outcome) => GPlayer
    ): GTeam =
      GTeam(m.team.gPlayers.map(playerVsTeam(_, m.opponents, m.outcome)))

    GMatchUpdate(
      teamVsTeam(gTeamMatch, playerVsTeam),
      teamVsTeam(gTeamMatch.inverse, playerVsTeam)
    )
  }

  /**
    * The Individual Update Method takes the naive approach to updating players after a game.
    * This method treats each match outcome as a set of wins or losses, one for each member of the opposing team.
    * This method takes advantage of rating systems like Elo or Glicko by abstracting
    * a team match as a set of 1­versus­1 matches.
    *
    * For example, consider a
    * game where Alice participates on a team versus an opposingteam consisting of
    * Bob, Carol, Dan, and Eve. If Alice’s team wins,
    * the game updates Alice’srating by considering this game as four wins:
    * one against Bob, one against Carol, one againstDan, and one against Eve.
    *
    */
  def evaluateIndividualUpdate(gTeamMatch: GTeamMatch): GMatchUpdate =
    evaluateMatch(
      gTeamMatch,
      (gPlayer: GPlayer, opponents: GTeam, outcome: Outcome) =>
        gPlayer.update(
          Glicko2.update(
            gPlayer.stats,
            opponents.gPlayers.map(gameResultType(outcome))
          )
        )
    )

  /**
    * The Composite Opponent Update Method considers each outcome as a match against asingle
    * player possessing the average rating and deviation of the opposing team players.
    * Ineffect, the update method creates a composite player out of the opposing team
    * and uses thisplayer’s resulting rating and deviation when updating a player.8
    *
    * For example, if Alice competes against a team consisting of Bob, Carol, Dan, and Eve,
    * the game will update Alice’s rating by considering the outcome as a 1­versus­1
    * match againsta composite player having the average rating and deviation of Bob, Carol, Dan, and Eve.
    *
    */
  def evaluateCompositeUpdate(gTeamMatch: GTeamMatch): GMatchUpdate =
    evaluateMatch(
      gTeamMatch,
      (gPlayer: GPlayer, opponents: GTeam, outcome: Outcome) =>
        gPlayer.update(
          Glicko2.update(
            gPlayer.stats,
            opponents.gPlayers
              .map(_.stats)
              .reduceOption(
                (p1, p2) =>
                  Player(
                    p1.rating + p2.rating,
                    p1.deviation + p2.deviation,
                    p1.volatility + p2.volatility
                  )
              )
              .map(
                p =>
                  Player(
                    p.rating / opponents.gPlayers.length,
                    p.deviation / opponents.gPlayers.length,
                    p.volatility / opponents.gPlayers.length
                  )
              )
              .map(GPlayer("composite-player", _))
              .map(gameResultType(outcome))
              .toSeq
          )
        )
    )

  def test(): Unit = {
    def defaultGPlayer(name: String): GPlayer = GPlayer(name, Glicko2.defaultPlayer())

    val p1 = defaultGPlayer("Alice")
    val p2 = defaultGPlayer("Bob")
    val p3 = defaultGPlayer("Carol")
    val p4 = defaultGPlayer("Dan")

    val mt = GTeamMatch(GTeam(Seq(p1, p2)), GTeam(Seq(p3, p4)), DRAW)

    println(s"$mt => ${evaluateIndividualUpdate(mt)}")
    println(s"$mt => ${evaluateCompositeUpdate(mt)}")

  }
}
