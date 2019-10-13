package com.sandtron.glicko2
import com.sandtron.glicko2.team.Glicko2Team
import com.sandtron.glicko2.team.store.impl.HsqlStore
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Parameters
import com.github.andriykuba.scala.glicko2.scala.Glicko2
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.{util => ju}
import com.sandtron.glicko2.team.storage.impl.BaseSqlStore
import scala.collection.mutable
import mr.sandtron.glicko2.team.storage.ReadOnlyGlicko2TeamStore
import com.sandtron.glicko2.team.storage.impl.CsvMatchHistoryStore
import java.nio.file.Paths

object Glicko2TeamApp {
  import com.sandtron.glicko2.team.Model._

  lazy val logger = LoggerFactory.getLogger(this.getClass())

  def configureLogger(suffix: String): Unit = {
    val context: LoggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]

    val logbackXml   = s"/logback-$suffix.xml"
    val configurator = new JoranConfigurator();
    configurator.setContext(context)
    context.reset()
    configurator.doConfigure(getClass.getResource(logbackXml))
  }

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm")
  implicit def str2Date(dateString: String): ju.Date = {
    println(s"formatting $dateString")
    dateFormatter.parse(dateString)
  }
  implicit def gplyr2String(gPlayer: GPlayer): String                  = gPlayer.name
  implicit def gplyrSeq2StringSeq(gPlayers: Seq[GPlayer]): Seq[String] = gPlayers.map(_.name)

  def computeAllMatches(db: ReadOnlyGlicko2TeamStore, matchEvaluator: GTeamMatch => GMatchUpdate): Seq[GPlayer] = {
    val matches  = db.loadMatches().toSeq
    val gplayers = new mutable.HashMap[String, GPlayer]()
    gplayers.addAll(
      matches
        .flatMap(m => m.team ++ m.opponents)
        .distinct
        .map(db.getGPlayer(_))
        .map(gp => gp.name -> gp)
    )
    matches.foreach(
      m =>
        matchEvaluator(m.toGTeamMatch(gplayers.values.toSeq)).gPlayers
          .map(gp => gp.name -> gp)
          .foreach(c => gplayers.update(c._1, c._2))
    )

    gplayers.values.toList

  }
  def main(args: Array[String]): Unit = {
    configureLogger("trace")
    // Glicko2Team.test()
    // val db = new HsqlStore("tempdb", Glicko2.defaultPlayer())
    val db = new CsvMatchHistoryStore(Paths.get("samples/input_matches.csv"), dateFormatter, Glicko2.defaultPlayer())

    // println(db.loadGPlayer("Alice"))
    val p1 = db.getGPlayer("Alice")
    val p2 = db.getGPlayer("Bob")
    val p3 = db.getGPlayer("Carol")
    val p4 = db.getGPlayer("Dave")

    println(computeAllMatches(db, Glicko2Team.evaluateCompositeUpdate))
    println(computeAllMatches(db, Glicko2Team.evaluateIndividualUpdate))
    // db.loadGPlayers().foreach(println)

    // db.addMatch(GMatchRecord(Seq(p1, p2), Seq(p3, p4), Outcome.WIN, "2019-10-11 12:00"))
    // db.addMatch(GMatchRecord(Seq(p1, p3), Seq(p2, p4), Outcome.LOSS, "2019-10-11 12:15"))
    // db.addMatch(GMatchRecord(Seq(p1, p4), Seq(p3, p2), Outcome.DRAW, "2019-10-12 13:00"))
    // db.addMatch(GMatchRecord(Seq(p1, p2), Seq(p3, p4), Outcome.WIN, "2019-10-12 13:20"))
    // db.loadMatches().foreach(println)

  }
}
