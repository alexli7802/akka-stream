import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "akka-stream",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.typesafe" % "config" % "1.3.2",
      "com.typesafe.akka" %% "akka-stream" % "2.5.14",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.14" % Test
    )
  ).settings(
    assemblyJarName in assembly := "akka-stream-examples.jar"
  )
