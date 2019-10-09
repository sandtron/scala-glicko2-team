package mr.sandtron.glicko2.team.storage
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player

trait Storage {
  def save(player: Player)
}
