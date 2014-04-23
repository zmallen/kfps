organization := "scala"

name := "kfps"

version := "1.0"

scalaVersion := "2.10.2"

exportJars := true

retrieveManaged := true

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.1"

libraryDependencies += "commons-net" % "commons-net" % "3.3"

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)
