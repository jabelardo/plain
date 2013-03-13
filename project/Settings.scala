/**
 *  Copyright (C) 2012 IBM
 */
package plain

import sbt._
import Keys._

import com.typesafe.sbt._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseKeys
import com.typesafe.sbteclipse.plugin.EclipsePlugin.EclipseCreateSrc
import com.github.retronym.SbtOneJar
import ls.Plugin.{ lsSettings, LsKeys }
import LsKeys.{ lsync, docsUrl => lsDocsUrl, tags => lsTags }

/**
  * Putting together all settings.
  */

object Settings {
  
  lazy val buildSettings = Seq(
    resolvers += "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "cc.spray releases" at "http://repo.spray.cc",
    organization := "com.ibm.plain",
    version      := "1.0.1-SNAPSHOT",
    scalaVersion := "2.10.1-RC3",
    logLevel     := Level.Info
  ) 

  lazy val baseSettings = Defaults.defaultSettings ++ buildSettings

  lazy val parentSettings = baseSettings ++ Seq(
    publishArtifact in Compile := false
  )

  lazy val sampleSettings = defaultSettings ++ onejarSettings ++ Seq(
    publishArtifact in Compile := false
  )

  lazy val defaultSettings = baseSettings ++ eclipseSettings ++ formatSettings ++ mimaSettings ++ lsSettings ++ Seq(

    scalacOptions in Compile ++= Seq(
		"-g:vars",
		"-optimize", 
		"-encoding", "UTF-8", 
		"-target:jvm-1.7", 
		"-deprecation", 
		"-feature", 
		"-unchecked", 
		"-Ydead-code",
		"-Yinline",
		"-Yinline-handlers",
		"-Yinline-warnings",
		"-Ywarn-adapted-args"
    ),
    
	javacOptions in Compile ++= Seq(
	    "-source", "1.7", 
	    "-target", "1.7", 
	    "-Xlint:unchecked", 
	    "-Xlint:deprecation"
	),

    description in lsync := "plain brings to PLM what Hana brings to SAP.",
    homepage in lsync := Some(url("http://www.ibm.com")),
    lsTags in lsync := Seq("plm", "inmemory"),
    lsDocsUrl in lsync := Some(url("http://www.ibm.com"))
  )

  lazy val eclipseSettings = Seq(
    EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource
  )

  lazy val formatSettings = SbtScalariform.scalariformSettings ++ Seq(
    SbtScalariform.ScalariformKeys.preferences in Compile := formattingPreferences,
    SbtScalariform.ScalariformKeys.preferences in Test    := formattingPreferences
  )

  def formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().setPreference(RewriteArrowSymbols, true)
  }

  lazy val mimaSettings = mimaDefaultSettings ++ Seq(
    previousArtifact := None
  )
  
  lazy val onejarSettings = SbtOneJar.oneJarSettings

}
