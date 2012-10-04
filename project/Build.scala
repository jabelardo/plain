/**
 *  Copyright (C) 2012 IBM
 */
package plain

import sbt._
import Keys._

import com.typesafe.sbtscalariform.ScalariformPlugin
import com.typesafe.sbtscalariform.ScalariformPlugin.ScalariformKeys
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
import ls.Plugin.{ lsSettings, LsKeys }
import LsKeys.{ lsync, docsUrl => lsDocsUrl, tags => lsTags }

object PlainBuild extends Build {
  
  /**
    * project or module structure
    */
  lazy val buildSettings = Seq(
    organization := "com.ibm.plain",
    version      := "1.0.1-SNAPSHOT",
    scalaVersion := "2.10.0-M7",
    logLevel     := Level.Warn
  ) 

  lazy val plain = Project(
    id = "plain",
    base = file("."),
    settings = parentSettings,
    aggregate = Seq(library, hybriddb, eai, monitorExtensionJmx)
  )

  def cpsPlugin = Seq(
    libraryDependencies <+= scalaVersion { v => compilerPlugin("org.scala-lang.plugins" % "continuations" % v) },
    scalacOptions += "-P:continuations:enable"
  )

  lazy val library = Project(
    id = "plain-library",
    base = file("plain-library"),
    settings = defaultSettings ++ cpsPlugin ++ Seq(
      autoCompilerPlugins := true,
      libraryDependencies ++= Dependencies.library
    )
  )

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
    dependencies = Seq(library),
    settings = sampleSettings ++ Seq(libraryDependencies ++= Dependencies.helloSample)
  )

  /**
   * Putting together all settings.
   */

  override lazy val settings = super.settings ++ buildSettings 

  lazy val baseSettings = Defaults.defaultSettings

  lazy val parentSettings = baseSettings ++ Seq(
    publishArtifact in Compile := false
  )

  lazy val sampleSettings = defaultSettings ++ Seq(
    publishArtifact in Compile := false
  )

  lazy val defaultSettings = baseSettings ++ formatSettings ++ mimaSettings ++ lsSettings ++ Seq(

    scalacOptions in Compile ++= Seq(
		"-optimize", 
		"-encoding", "UTF-8", 
		"-target:jvm-1.7", 
		"-deprecation", 
		"-feature", 
		"-unchecked", 
		"-Xlog-reflective-calls", 
		"-Ywarn-adapted-args"),
    
	javacOptions in Compile ++= Seq("-source", "1.7", "-target", "1.7", "-Xlint:unchecked", "-Xlint:deprecation"),

    description in lsync := "plain brings to PLM what Hana brings to SAP.",
    homepage in lsync := Some(url("http://www.ibm.com")),
    lsTags in lsync := Seq("plm", "inmemory"),
    lsDocsUrl in lsync := Some(url("http://www.ibm.com"))
  )

  lazy val formatSettings = ScalariformPlugin.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := formattingPreferences,
    ScalariformKeys.preferences in Test    := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
  }

  lazy val mimaSettings = mimaDefaultSettings ++ Seq(
    previousArtifact := None
  )

  }


/**
 * All dependencies to 3rd party libraries.
 */  
object Dependencies {

  import Dependency._

  val library = Seq(config)
  val helloSample = library		

}

object Dependency {

  /**
    * At compile time.
    */
  val config        = "com.typesafe"                % "config"                       % "0.5.2"       // ApacheV2

  /**
    * During testing.
    */
  object Test {
    
    val commonsMath = "org.apache.commons"          % "commons-math"                 % "2.1"              % "test" // ApacheV2
    val commonsIo   = "commons-io"                  % "commons-io"                   % "2.0.1"            % "test" // ApacheV2
    val junit       = "junit"                       % "junit"                        % "4.10"             % "test" // Common Public License 1.0
    val logback     = "ch.qos.logback"              % "logback-classic"              % "1.0.4"            % "test" // EPL 1.0 / LGPL 2.1
    val mockito     = "org.mockito"                 % "mockito-all"                  % "1.8.1"            % "test" // MIT
    val scalatest   = "org.scalatest"               % "scalatest"                    % "1.9-2.10.0-M7-B1" % "test" cross CrossVersion.full // ApacheV2
    val scalacheck  = "org.scalacheck"              % "scalacheck"                   % "1.10.0"           % "test" cross CrossVersion.full // New BSD

  }
  
}

