package mr.sandtron.glicko2.team.storage
import com.sandtron.glicko2.team.Model.GPlayer
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GTeamMatch
import java.time.LocalDateTime
import com.sandtron.glicko2.team.Model.GTeam
import com.sandtron.glicko2.team.Model.GMatchRecord
import java.{util => ju}

trait Glicko2TeamStore extends ReadOnlyGlicko2TeamStore with WriteOnlyGlicko2TeamStore
trait ReadOnlyGlicko2TeamStore {

  /**
    * gets the default player stats
    */
  def defaultPlayer(): Player

  /**
    * finds a player in the database by name or generates the default one
    */
  def getGPlayer(name: String): GPlayer

  /**
    * loads all matches in database ordered by CREATED_TIME ASC
    */
  def loadMatches(): Iterator[GMatchRecord]

  /**
    * loads all matches in database after the given time ordered by CREATED_TIME ASC
    */
  def loadMatches(start: ju.Date): Iterator[GMatchRecord]

}
trait WriteOnlyGlicko2TeamStore {

  /**
    * Loads all players from the database
    */
  def loadGPlayers(): Iterator[GPlayer]

  /**
    * creates or updates a given player
    */
  def upsertGPlayer(gPlayer: GPlayer)

  /**
    * adds the given GMatchRecord to the database
    */
  def addMatch(gTeamMatch: GMatchRecord): Unit

  /**
    * adds the given GMatchRecords to the database
    */
  def addMatches(matches: Seq[GMatchRecord]): Unit = matches.foreach(addMatch)

}
