/**
 *  Copyright (C) 2012 IBM
 */
package plain

import sbt._
import Keys._

object PlainBuild extends Build {

  import Settings._
  import Dependencies._
  
  /**
    * project or module structure
    */
  lazy val root = Project("root", file("."))
    .aggregate(library, hybriddb, eai, monitorExtensionJmx)
    .settings(parentSettings: _*)

  lazy val library = Project("plain-library", file("plain-library"))
    .settings(defaultSettings: _*)
    .settings(cpsPlugin: _*)
    .settings(libraryDependencies ++= provided(akkaActor) ++ compile(config) ++ test(junit))

  lazy val monitorExtensionJmx = Project(
    id = "plain-monitor-extension-jmx",
    base = file("plain-monitor-extension-jmx"),
    settings = defaultSettings ++ cpsPlugin
  )

  lazy val hybriddb = Project(
    id = "plain-hybriddb",
    base = file("hybriddb"),
    dependencies = Seq(library),
    settings = defaultSettings ++ cpsPlugin
  )

  lazy val model = Project(
    id = "plain-model",
    base = file("plain-model"),
    dependencies = Seq(hybriddb),
    settings = defaultSettings ++ cpsPlugin
  )

  lazy val eai = Project(
    id = "plain-eai",
    base = file("plain-eai"),
    dependencies = Seq(library),
    settings = defaultSettings ++ cpsPlugin
  )

  lazy val samples = Project(
    id = "plain-samples",
    base = file("plain-samples"),
    settings = parentSettings,
    aggregate = Seq(helloSample)
  )

  lazy val helloSample = Project(
    id = "plain-sample-hello-world",
    base = file("plain-samples/plain-sample-hello-world"),
    dependencies = Seq(library)
  )

  def cpsPlugin = Seq(
    libraryDependencies <+= scalaVersion { v => compilerPlugin("org.scala-lang.plugins" % "continuations" % v) },
    scalacOptions += "-P:continuations:enable"
  )
  
}

