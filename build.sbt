import AssemblyKeys._

name := "reading-service"

scalaVersion  := "2.11.4"

// This is needed due to a bug in the scala reflection that makes tests intermittently fail.
// See: https://issues.scala-lang.org/browse/SI-6240
lazy val testSettings = Seq(
  parallelExecution in Test := false
)

lazy val buildSettings = Seq(
  organization := "com.blinkbox.books.agora",
  scalaVersion  := "2.11.4",
  version := scala.util.Try(scala.io.Source.fromFile("VERSION").mkString.trim).getOrElse("0.0.0"),
  scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8", "-target:jvm-1.7")
)

lazy val artifactSettings = addArtifact(artifact in (Compile, assembly), assembly).settings

lazy val common = (project in file("common")).settings(buildSettings: _*)

lazy val root = (project in file(".")).
  dependsOn(public, admin).aggregate(public, admin).
  settings(buildSettings: _*).
  settings(publish := {})

lazy val public = (project in file("public")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)

lazy val admin = (project in file("admin")).
  dependsOn(common % "compile->compile;test->test").aggregate(common).
  settings(aggregate in publish := false).
  settings(buildSettings: _*).
  settings(rpmPrepSettings: _*).
  settings(artifactSettings: _*)
