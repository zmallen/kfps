organization := "scala"

name := "kfps"

mainClass:= Some("scala.KindaFastPingSweep")

version := "1.0"

scalaVersion := "2.10.2"

exportJars := true

retrieveManaged := true

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.1"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)
