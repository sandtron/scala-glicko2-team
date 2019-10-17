package com.sandtron.glicko2.team
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
import scala.util.Using
import scala.collection.JavaConverters._
import com.sandtron.glicko2.team.app.com.sandtron.glicko2.config.Config
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Player
import java.nio.file.Files
import java.io.File

object Glicko2TeamApp {
  import com.sandtron.glicko2.team.Model._

  lazy val logger = LoggerFactory.getLogger(this.getClass())

  def configureLogger(logbackPath: String): Unit = {
    val context: LoggerContext = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]

    val configurator = new JoranConfigurator();
    configurator.setContext(context)
    context.reset()

    configurator.doConfigure(logbackPath)
  }

  implicit def gplyr2String(gPlayer: GPlayer): String                  = gPlayer.name
  implicit def gplyrSeq2StringSeq(gPlayers: Seq[GPlayer]): Seq[String] = gPlayers.map(_.name)

  def computeAllMatches(
      db: ReadOnlyGlicko2TeamStore,
      ratingPeriodGames: Int,
      matchEvaluator: GTeamMatch => GMatchUpdate
  ): Seq[GPlayer] = {
    val allMatches  = db.loadMatches().toSeq
    val allGPlayers = new mutable.HashMap[String, GPlayer]()

    allMatches
      .sliding(ratingPeriodGames)
      .foreach(matches => {
        // add any new players to the list of all players
        allGPlayers
          .addAll(
            matches
              .flatMap(_.gPlayers)
              .distinct
              .filter(!allGPlayers.keySet.contains(_))
              .map(db.getGPlayer(_))
              .map(gp => gp.name -> gp)
          )

        // handle games that have been played this period
        matches.foreach(
          m =>
            matchEvaluator(m.toGTeamMatch(allGPlayers.values.toSeq)).gPlayers
              .map(gp => gp.name -> gp)
              .foreach(c => allGPlayers.update(c._1, c._2))
        )

        // handle decay of players who have not played in this period but have played before.
        val hasPlayed: Seq[String] = matches.flatMap(m => m.team ++ m.opponents)

        allGPlayers
          .filterKeys(!hasPlayed.contains(_))
          .values
          .foreach(p => allGPlayers.update(p.name, p.update(Glicko2.update(p.stats))))
      })

    allGPlayers.values.toList

  }
  def main(args: Array[String]): Unit = {
    val cfgFile = args.headOption.getOrElse("glicko2team.properties")

    Using(scala.io.Source.fromFile(cfgFile).bufferedReader())(System.getProperties().load).get

    val props = System.getProperties().asScala

    props.get(System.getProperty("logback.path")).foreach(configureLogger)
    logger.info("logger configured")
    val gplayers = computeAllMatches(
      new CsvMatchHistoryStore(
        Paths.get(props.get("matchSource.path").get),
        new SimpleDateFormat(props.get("matchSource.csv.dateFormat").get),
        Player(
          BigDecimal(props.get("player.default.rating").get),
          BigDecimal(props.get("player.default.deviation").get),
          BigDecimal(props.get("player.default.volatility").get)
        )
      ),
      props.get("glicko2.team.ratingPeriod.games").flatMap(_.toIntOption).getOrElse(15),
      props.get("glicko2.team.algorithm").map(_.toUpperCase) match {
        case Some("COMPOSITE")  => Glicko2Team.evaluateCompositeUpdate
        case None               => Glicko2Team.evaluateCompositeUpdate
        case Some("INDIVIDUAL") => Glicko2Team.evaluateIndividualUpdate
        case _ =>
          throw new RuntimeException(
            s"glicko2.team.algorithm must be 'COMPOSITE' or 'INDIVIDUAL' or not provided"
          )
      }
    )
    gplayers.foreach(println)

  }
}
