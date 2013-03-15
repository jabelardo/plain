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
    .aggregate(library, hybriddb, eai, monitorExtensionJmx, samples)
    .settings(parentSettings: _*)

  lazy val library = Project("plain-library", file("plain-library"))
    .settings(defaultSettings: _*)
    .settings(cpsPlugin: _*)
    .settings(libraryDependencies ++= 
      compile(
          scalareflect,
          config, 
          logback, 
          janino, 
          akkaSlf4j, 
          akkaActor, 
          commonsLang, 
          commonsCodec, 
          commonsIo, 
          clHashMap, 
          fasterXml,
          jerseyJson,
			derbyjdbc,
			oraclejdbc,
			mysqljdbc,
			reflections
      ) ++ 
      test(junit, junitItf)
  )

  lazy val monitorExtensionJmx = Project(
    id = "plain-monitor-extension-jmx",
    base = file("plain-monitor-extension-jmx"),
    settings = defaultSettings ++ cpsPlugin
  )

  lazy val hybriddb = Project(
    id = "plain-hybriddb",
    base = file("plain-hybriddb"),
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

  lazy val samples = Project("plain-sample", file("plain-sample"))
    .aggregate(helloSample)
    .aggregate(jdbcSample)

  lazy val helloSample = Project("plain-sample-hello-world", file("plain-sample/plain-sample-hello-world"))
    .settings(sampleSettings: _*)
    .dependsOn(library)

  lazy val jdbcSample = Project("plain-sample-jdbc", file("plain-sample/plain-sample-jdbc"))
    .settings(sampleSettings: _*)
    .dependsOn(library)

  def cpsPlugin = Seq(
    libraryDependencies <+= scalaVersion { v => compilerPlugin("org.scala-lang.plugins" % "continuations" % v) },
    scalacOptions += "-P:continuations:enable"
  )
  
}
