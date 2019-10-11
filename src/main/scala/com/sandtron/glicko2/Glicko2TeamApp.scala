package com.sandtron.glicko2
import com.sandtron.glicko2.team.Glicko2Team
import com.sandtron.glicko2.team.store.impl.HsqlStore
import com.github.andriykuba.scala.glicko2.scala.Glicko2.Parameters
import com.github.andriykuba.scala.glicko2.scala.Glicko2

object Glicko2TeamApp {
  def main(args: Array[String]): Unit = {

    // Glicko2Team.test()
    val db = new HsqlStore("tempdb", Glicko2.defaultPlayer())
    db.init()

    val p1 = db.loadOrCreateGPlayer("Alice")
    val p2 = db.loadOrCreateGPlayer("Bob")
    val p3 = db.loadOrCreateGPlayer("Carol")
    val p4 = db.loadOrCreateGPlayer("Dave")
    
    db.loadGPlayers().foreach(println)
  }
}
