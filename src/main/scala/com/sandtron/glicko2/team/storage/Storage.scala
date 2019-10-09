package mr.sandtron.glicko2.team.storage
import com.sandtron.glicko2.team.Model.GPlayer
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GTeamMatch
import java.time.LocalDateTime

trait GPlayerStorage {
  def defaultPlayer(): Player

  /**
    * save a player to persisted storage.
    * The player id must be unique
    */
  def save(player: GPlayer): GPlayer

  def createGPlayer(name: String, player: Player): GPlayer

  def createGPlayer(name: String): GPlayer = createGPlayer(name, defaultPlayer())

  def createGPlayer(name: String, rating: BigDecimal, deviation: BigDecimal, volatility: BigDecimal): GPlayer =
    createGPlayer(name, Player(rating, deviation, volatility))

  /**
    * load player by playerId
    */
  def loadGPlayer(name: String): Option[GPlayer]

  def createOrLoadGPlayer(name: String): GPlayer = loadGPlayer(name).getOrElse(createGPlayer(name))

  def loadGPlayers(): Seq[GPlayer]
}
trait GTeamMatchStorage {

  def addMatches(matches: Seq[GTeamMatch]): Unit

  def loadMatches(): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, end: LocalDateTime): Seq[GTeamMatch]

  def loadMatches(gPlayers: GPlayer*): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch]

  def loadMatches(start: LocalDateTime, end: LocalDateTime, gPlayers: GPlayer*): Seq[GTeamMatch]

}
