package com.sandtron.glicko2.team.app

package com.sandtron.glicko2.config
import java.text.DateFormat
import java.nio.file.Path
import java.nio.file.Paths
import mr.sandtron.glicko2.team.storage.ReadOnlyGlicko2TeamStore
import _root_.com.sandtron.glicko2.team.Glicko2Team
import _root_.com.sandtron.glicko2.team.Model.GMatchUpdate
import _root_.com.sandtron.glicko2.team.Model.GTeamMatch
import _root_.com.sandtron.glicko2.team.storage.impl.CsvMatchHistoryStore
import _root_.com.github.andriykuba.scala.glicko2.scala.Glicko2
import java.text.SimpleDateFormat

trait Config {
  lazy val df = new SimpleDateFormat("yyyy-MM-dd HH:mm")

  def readStore: ReadOnlyGlicko2TeamStore
  def logback: String
  def dateFormat: SimpleDateFormat          = df
  def algorithm: GTeamMatch => GMatchUpdate = Glicko2Team.evaluateCompositeUpdate
  def appDir: Path                          = Paths.get("glicko2team")
}
