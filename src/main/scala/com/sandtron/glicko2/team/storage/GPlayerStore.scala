package mr.sandtron.glicko2.team.storage
import com.sandtron.glicko2.team.Model.GPlayer
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GTeamMatch
import java.time.LocalDateTime
import com.sandtron.glicko2.team.Model.GTeam

trait GPlayerStore {
  def defaultPlayer(): Player

  /**
    * save a player to persisted storage.
    * The player id must be unique
    */
  def updateGPlayer(player: GPlayer): GPlayer

  /**
    * load player by playerId
    */
  def loadGPlayer(name: String): Option[GPlayer]

  def createOrUpdate(gPlayer: GPlayer): GPlayer = loadGPlayer(gPlayer.name) match {
    case Some(gPlayer) => updateGPlayer(gPlayer)
    case None          => createGPlayer(gPlayer.name, gPlayer.stats)
  }

  def createGPlayer(name: String, player: Player): GPlayer

  def createGPlayer(name: String): GPlayer = createGPlayer(name, defaultPlayer())

  def createGPlayer(name: String, rating: BigDecimal, deviation: BigDecimal, volatility: BigDecimal): GPlayer =
    createGPlayer(name, Player(rating, deviation, volatility))

  def loadOrCreateGPlayer(name: String): GPlayer = loadGPlayer(name).getOrElse(createGPlayer(name))

  def loadGPlayers(): Seq[GPlayer]
}
trait GTeamMatchStore {
  def addMatch(gTeamMatch: GTeamMatch): Unit

  def addMatches(matches: Seq[GTeamMatch]): Unit = matches.foreach(addMatch)

  def loadMatches(): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, end: LocalDateTime): Seq[GTeamMatch]

  def loadMatches(gPlayers: GPlayer*): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, end: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch]

}

