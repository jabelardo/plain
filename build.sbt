import sbt._
import Keys._
import EclipseKeys._
import Core._
import Integration._
import Cqrs._
import Samples._

name := "plain.io"

organization in ThisBuild := "com.ibm.plain"

scalaVersion in ThisBuild := "2.11.1"

mainClass in (Compile, run) := Some("com.ibm.plain.bootstrap.Main")

publishTo in ThisBuild := { 
	def repo = if (version.value.trim.endsWith("SNAPSHOT")) "libs-snapshot-local" else "libs-release-local"
	Some("Artifactory Realm" at "http://pdmbuildvm.munich.de.ibm.com:8080/artifactory/" + repo)
}

credentials in ThisBuild += Credentials(Path.userHome / ".ivy2" / ".credentials")

createSrc in ThisBuild := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

eclipseOutput in ThisBuild := Some("target/scala-2.11/classes")

withSource in ThisBuild:= true

incOptions := incOptions.value.withNameHashing(true)

scalacOptions in (doc) := Seq("-diagrams", "-doc-title plain.io", "-access private")

scalacOptions in ThisBuild ++= Seq(
	"-g:vars",
	"-encoding", "UTF-8", 
	"-target:jvm-1.7", 
	"-deprecation", 
	"-feature", 
	"-Yinline-warnings",
	"-Yno-generic-signatures",
	"-optimize",
	"-unchecked"
)

javacOptions in ThisBuild ++= Seq(
	"-source", "1.8",
	"-target", "1.8",
	"-Xlint:unchecked",
	"-Xlint:deprecation",
	"-Xlint:-options"
)

scalariformSettings

graphSettings

lazy val allSettings = runSettings ++ scalariformSettings ++ integrationSettings ++ graphSettings

lazy val runSettings = Seq(fork in (Compile, run) := false)

// libary projects

lazy val `plain-core` = project in file("plain-core") settings(plainSettings ++ graphSettings: _*)

lazy val `plain-cqrs` = project in file("plain-cqrs") dependsOn `plain-core` settings(jdbcSettings: _*)

lazy val `plain-integration` = project in file("plain-integration") dependsOn `plain-core` settings(integrationSettings ++ graphSettings: _*)

// sample projects

lazy val helloworld = project in file("samples/helloworld") dependsOn `plain-core` settings(allSettings: _*)

lazy val jdbc = project in file("samples/jdbc") dependsOn `plain-core` settings(jdbcSettings: _*) settings(allSettings: _*)

lazy val servlet = project in file("samples/servlet") dependsOn `plain-core` settings(allSettings: _*)

lazy val integrationserver = project in file("samples/integrationserver") dependsOn `plain-integration` settings(allSettings: _*)

lazy val integrationclient = project in file("samples/integrationclient") dependsOn `plain-integration` settings(allSettings: _*)

// techempower framework benchmark

lazy val `plain-benchmark` = project in file("samples/benchmark") dependsOn `plain-core` settings(jdbcSettings: _*) settings(allSettings: _*)


