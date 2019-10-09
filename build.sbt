import Dependencies._

ThisBuild / scalaVersion := "2.13.0"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "mr.sandtron"
ThisBuild / organizationName := "glicko2"

// lazy val root = (project in file("."))
//   .settings(
//     name := "scala-glicko2-team"
//   )
libraryDependencies += "org.mockito"   % "mockito-core" % "2.22.0" % Test
libraryDependencies += "junit"         % "junit"        % "4.12"   % Test
libraryDependencies += "org.slf4j"     % "slf4j-simple" % "1.7.25" % Test
libraryDependencies += "org.scalatest" %% "scalatest"   % "3.0.8"  % Test
