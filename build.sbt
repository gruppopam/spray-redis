assemblySettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

organization in ThisBuild := "com.thoughtworks"

name := """spray-redis"""

version := "1.0"

scalaVersion := "2.11.2"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8", "-language:postfixOps", "-Yrangepos")

resolvers += "spray repo" at "http://repo.spray.io"

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers ++= Seq("snapshots", "releases").map(Resolver.sonatypeRepo)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.1.4" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "io.spray" %% "spray-can" % "1.3.1",
  "io.spray" %% "spray-caching" % "1.3.1",
  "io.spray" %% "spray-json" % "1.2.6",
  "net.debasishg" %% "redisreact" % "0.6"
)