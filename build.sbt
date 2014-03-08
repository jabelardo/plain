import sbt._
import Keys._
import EclipseKeys._

scalariformSettings

name := "plain.io"

organization in ThisBuild := "com.ibm"

scalaVersion in ThisBuild := "2.10.3"

version in ThisBuild := "1.0.0-SNAPSHOT"

mainClass in ThisBuild := Some("com.ibm.plain.bootstrap.Main")

createSrc in ThisBuild := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

eclipseOutput := Some("target")

withSource := true

scalacOptions in ThisBuild ++= Seq(
	"-g:vars",
	"-encoding", "UTF-8", 
	"-target:jvm-1.7", 
	"-deprecation", 
	"-feature", 
	"-unchecked",
	"-optimise"
)

javacOptions in ThisBuild ++= Seq(
	"-source", "1.7",
	"-target", "1.7",
	"-Xlint:unchecked",
	"-Xlint:deprecation",
	"-Xlint:-options"
)

lazy val library = project in file("plain-library")

lazy val hybriddb = project in file("plain-hybriddb") dependsOn "library"

lazy val samples = project in file("plain-samples") aggregate ("helloworld", "jdbc")

lazy val helloworld = project in file("plain-samples/plain-sample-hello-world") dependsOn "library"

lazy val jdbc = project in file("plain-samples/plain-sample-jdbc") dependsOn "library" 

lazy val benchmark = project in file("plain-benchmark") dependsOn "library"


