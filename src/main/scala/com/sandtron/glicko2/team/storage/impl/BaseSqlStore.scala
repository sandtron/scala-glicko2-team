package com.sandtron.glicko2.team.storage.impl
import scala.util.Using
import scala.util.Failure
import scala.io.Source._
import java.{util => ju}
import scala.util.Success
import scala.collection.JavaConverters._
import scala.util.Using.Releasable
import java.sql.Connection
import scala.util.Try
import java.sql.ResultSet
import java.sql.DriverManager
import org.slf4j.LoggerFactory
import javax.sql.rowset._
import scala.collection.mutable
import com.sandtron.glicko2.team.Model.GPlayer
import com.sandtron.glicko2.team.Model.GTeam
import java.sql.Date

object BaseSqlStore {
  lazy val logger = LoggerFactory.getLogger(classOf[BaseSqlStore])
  trait LazyEntity[A] {
    def entity: A
  }
  class LazyGTeam(team: => Seq[GPlayer]) extends GTeam with LazyEntity[Seq[GPlayer]] {
    var loaded                      = false;
    lazy val entity                 = { loaded = true; team }
    lazy val gPlayers               = entity
    override def toString(): String = s"LazyGTeam[${if (loaded) gPlayers else "NOT LOADED"}]"
  }
  object implicits {

    implicit class RichRs(rs: ResultSet) {
      // TODO: find a better way to this that doesn't cache everything
      def map[A](mapper: ResultSet => A): Iterator[A] = {
        val mapped = new mutable.ListBuffer[A]()
        while (rs.next()) {
          mapped.addOne(mapper(rs))
        }
        rs.close()
        mapped.toStream.iterator
      }

      def flatMap[A](mapper: ResultSet => Iterator[A]): Iterator[A] = rs.map(mapper).flatten
    }

  }

}
abstract class BaseSqlStore {

  import BaseSqlStore.implicits._

  private lazy val logger = BaseSqlStore.logger

  lazy val queries = Using(fromURL(getClass.getResource("/base-sql-queries.properties")).bufferedReader())(
    reader => {
      val p = new ju.Properties()
      p.load(reader)
      p.asScala
    }
  ) match {

    case Success(props)        => props
    case Failure(e: Throwable) => throw e
  }

  def init(): Unit = {
    queries.iterator
      .filter(_._1.startsWith("db.config.create"))
      .map {
        case (propName, sql) => sql
      }
      .foreach(sql => usingCon(c => c.prepareStatement(sql).executeUpdate()).get)
  }
  protected def use[R: Releasable, A](resource: => R)(f: R => A): Try[A] =
    Using(resource)(f)

  protected def usingCon[A](withConFun: Connection => A): Try[A]

  protected def createTableIfNotExists(tableName: String, creationQuery: String) = {
    usingCon(c => {
      Using(c.getMetaData().getTables(null, null, tableName, null))(rs => {
        var result = false;

        while (rs.next() && !result) {
          if (rs.getString("TABLE_NAME") == tableName) {
            println(s"found table $tableName")
            result = true
          }
        }
        if (!result) {
          println(s"creating table $tableName: $creationQuery")
          c.prepareStatement(creationQuery).executeUpdate()
        }

        result
      })
    }).get
  }

  protected def getAllDataFromTable[A](tableName: String)(rsHandler: ResultSet => A): A =
    rsHandler(select(s"SELECT * FROM $tableName"))

  protected def select[A](selectStatement: String): ResultSet =
    usingCon(c => {
      logger.trace("executing {}", selectStatement)
      c.prepareStatement(selectStatement).executeQuery
    }).get

  protected def select[A](selectStatement: String, params: Any*): ResultSet =
    usingCon(c => {
      val ps = c.prepareStatement(selectStatement)
      processParams(params).indices.foreach(n => ps.setObject(n + 1, params(n)))
      logger.trace("executing {}, params=[{}]", selectStatement, params.mkString(","))
      ps.executeQuery()
    }).get

  protected def upsert(upsertStmt: String, params: Any*) =
    usingCon(con => {
      logger.debug(s"executing $upsertStmt with ${params.map(p => s"[$p:${p.getClass().getSimpleName()}]")}")

      val ps = con.prepareStatement(upsertStmt)
      processParams(params).indices.foreach(n => ps.setObject(n + 1, params(n)))
      logger.debug("executing upsert {}, params=[{}]", upsertStmt, params)
      ps.executeUpdate()
    }).get

  protected def processParams(params: Seq[Any]): Seq[Any] =
    params
      .map(_ match {
        case d: ju.Date => new Date(d.getTime)
        case x: Any     => x
      })
      .toList
}
