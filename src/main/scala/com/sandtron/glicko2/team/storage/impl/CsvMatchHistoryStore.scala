package com.sandtron.glicko2.team.storage.impl
import java.nio.file.Path
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import com.sandtron.glicko2.team.Model.GPlayer
import com.sandtron.glicko2.team.Model.GMatchRecord
import mr.sandtron.glicko2.team.storage.ReadOnlyGlicko2TeamStore
import java.{util => ju}
import scala.util.Using
import scala.io.Source
import scala.collection.mutable
import com.sandtron.glicko2.team.Model.Outcome
import java.text.DateFormat

class CsvMatchHistoryStore(csvFile: Path, dateFormatter: DateFormat, val defaultPlayer: Player)
    extends ReadOnlyGlicko2TeamStore {
  private def parseCsv(): Iterator[GMatchRecord] = {
    val headers = new mutable.ListBuffer[String]()
    Using(Source.fromFile(csvFile.toFile()))(r => {
      val lines = r.getLines()
      if (lines.hasNext) {
        headers.addAll(lines.next().split(",").map(_.toUpperCase()))
      }
      lines
        .filter(!_.isEmpty())
        .map(line => line.split(","))
        .toList
        .map(
          row =>
            GMatchRecord(
              row(headers.indexOf("TEAM1")).split("\\|"),
              row(headers.indexOf("TEAM2")).split("\\|"),
              Outcome.withName(row(headers.indexOf("OUTCOME"))),
              dateFormatter.parse(row(headers.indexOf("DATETIME")))
            )
        )
        .iterator
    }).get
  }

  /**
    * finds a player in the database by name or generates the default one
    */
  def getGPlayer(name: String): GPlayer = GPlayer(name, defaultPlayer)

  /**
    * loads all matches in database ordered by CREATED_TIME ASC
    */
  def loadMatches(): Iterator[GMatchRecord] =
    parseCsv().toSeq
      .sortBy(m => m.gameTime)
      .iterator

  /**
    * loads all matches in database after the given time ordered by CREATED_TIME ASC
    */
  def loadMatches(start: ju.Date): Iterator[GMatchRecord] =
    loadMatches().filter(m => m.gameTime.getTime > start.getTime())
}
