/**
 *  Copyright (C) (R) 2013 IBM
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
    .aggregate(library, hybriddb, samples)
    .settings(parentSettings: _*)

  lazy val library = Project("plain-library", file("plain-library"))
    .settings(defaultSettings: _*)
    .settings(libraryDependencies ++= 
      compile(
          // slf4jnoop, // uncomment to switch off all logging 
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
		  derbyclient,
		  h2jdbc,
		  mysqljdbc,
		  reflections
      ) ++ test(junit, junitItf)
  )

  lazy val hybriddb = Project(
    id = "plain-hybriddb",
    base = file("plain-hybriddb"),
    dependencies = Seq(library),
    settings = library.settings
  )

  lazy val samples = Project("plain-sample", file("plain-sample"))
    .aggregate(helloSample)
    .aggregate(jdbcSample)

  lazy val helloSample = Project("plain-sample-hello-world", file("plain-sample/plain-sample-hello-world"))
    .settings(sampleSettingsResourceOnly: _*)
    .dependsOn(library)

  lazy val jdbcSample = Project(
    id = "plain-sample-jdbc", 
    base = file("plain-sample/plain-sample-jdbc"),
    settings = sampleSettingsResourceOnly,
    dependencies = Seq(library)
  )

}
