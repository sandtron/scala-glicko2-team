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
abstract class BaseSqlStore {
  lazy val queries = {

    Using(fromURL(getClass.getResource("/base-sql-queries.properties")).bufferedReader())(
      reader => {
        val p = new ju.Properties()
        p.load(reader)
        p.asScala
      }
    ) match {

      case Success(props)        => props
      case Failure(e: Throwable) => throw e
    }
  }
  implicit class RichRs(val rs: ResultSet) {
    def unsafeIterate(): Iterator[ResultSet] = new Iterator[ResultSet] {
      def hasNext: Boolean           = rs.next()
      def next(): java.sql.ResultSet = rs
    }
    def unsafeHeadOption(): Option[ResultSet] = if (rs.next()) Some(rs) else None
  }

  def init(): Unit = queries.iterator.filter(_._1.startsWith("create.table.")).foreach {
    case (propName, sql) => createTableIfNotExists(propName.split("\\.")(2), sql)
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

  protected def getAllDataFromTable[A](tableName: String)(rsHandler: ResultSet => A): Try[A] =
    select(s"SELECT * FROM $tableName")(rsHandler)

  protected def select[A](selectStatement: String)(handler: ResultSet => A): Try[A] =
    usingCon(c => c.prepareStatement(selectStatement).executeQuery).map(handler)

  protected def select[A](selectStatement: String, params: Any*)(handler: ResultSet => A): Try[A] =
    usingCon(c => {
      val ps = c.prepareStatement(selectStatement)
      params.indices.foreach(n => ps.setObject(n + 1, params(n)))
      ps.executeQuery()
    }).map(handler)

  protected def upsert(upsertStmt: String, params: Any*) =
    usingCon(con => {
      println(s"executing $upsertStmt with ${params.map(p => s"[$p:${p.getClass().getSimpleName()}]")}")

      val ps = con.prepareStatement(upsertStmt)
      params.indices.foreach(n => ps.setObject(n + 1, params(n)))
      println("executing upsert")
      ps.executeUpdate()
    }).get
}
