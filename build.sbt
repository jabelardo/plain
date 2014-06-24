import sbt._
import Keys._
import EclipseKeys._
import Plain._
import Integration._
import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

name := "plain.io"

organization in ThisBuild := "com.ibm.plain"

scalaVersion in ThisBuild := "2.11.1"

version in ThisBuild := "1.0.0-SNAPSHOT"

mainClass in (Compile, run) := Some("com.ibm.plain.bootstrap.Main")

val runSettings = Seq(fork in (Compile, run) := false)

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

// libary projects

lazy val library = project in file("plain-library") settings(plainSettings ++ graphSettings: _*)

lazy val hybriddb = project in file("plain-hybriddb") dependsOn library settings(jdbcSettings: _*)

lazy val integration = project in file("plain-integration") dependsOn library settings(integrationSettings ++ graphSettings: _*)

lazy val migration = project in file("plain-migration") dependsOn integration settings(integrationSettings ++ graphSettings: _*)

lazy val coexistence = project in file("plain-coexistence") dependsOn integration settings(integrationSettings ++ graphSettings: _*)

// techempower framework benchmark

lazy val benchmark = project in file("plain-benchmark") dependsOn library settings(jdbcSettings: _*) settings(allSettings: _*)

// sample projects

lazy val samples = project in file("plain-samples") aggregate(helloworldsample, jdbcsample, integrationserversample, integrationclientsample)  

lazy val helloworldsample = project in file("plain-samples/plain-sample-hello-world") dependsOn library settings(allSettings: _*)

lazy val jdbcsample = project in file("plain-samples/plain-sample-jdbc") dependsOn library settings(jdbcSettings: _*) settings(allSettings: _*)

lazy val servletsample = project in file("plain-samples/plain-sample-servlet") dependsOn library settings(allSettings: _*)

lazy val integrationserversample = project in file("plain-samples/plain-integration-server") dependsOn integration settings(allSettings: _*)

lazy val integrationclientsample = project in file("plain-samples/plain-integration-client") dependsOn integration settings(allSettings: _*)

// MAN specific projects

lazy val `man-migration-framework` = project in file("man-migration-framework") dependsOn (migration, coexistence) settings(allSettings: _*)


