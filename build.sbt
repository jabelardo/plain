import sbt._
import Keys._
import EclipseKeys._
import AssemblyKeys._
import Plain._
import Camel._

name := "plain.io"

organization in ThisBuild := "com.ibm"

scalaVersion in ThisBuild := "2.11.0"

version in ThisBuild := "1.0.0-SNAPSHOT"

val bootstrapMain = Some("com.ibm.plain.bootstrap.Main")

val runSettings = Seq(
  mainClass in (Compile, run) := bootstrapMain,
  fork in (Compile, run) := false
)

mainClass in ThisBuild := bootstrapMain

createSrc in ThisBuild := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

eclipseOutput in ThisBuild := Some("target/scala-2.11/classes")

withSource in ThisBuild:= true

scalacOptions in (doc) := Seq("-diagrams", "-doc-title plain.io")

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

incOptions := incOptions.value.withNameHashing(true) 

javacOptions in ThisBuild ++= Seq(
	"-source", "1.7",
	"-target", "1.7",
	"-Xlint:unchecked",
	"-Xlint:deprecation",
	"-Xlint:-options"
)

scalariformSettings

graphSettings

lazy val library = project in file("plain-library") settings(plainSettings ++ graphSettings: _*)

lazy val hybriddb = project in file("plain-hybriddb") dependsOn library settings(jdbcSettings: _*)

lazy val samples = project in file("plain-samples") aggregate(helloworld, jdbc, integrationhelloworld, preparation)  

lazy val helloworld = project in file("plain-samples/plain-sample-hello-world") dependsOn library settings(assemblySettings: _*)

lazy val jdbc = project in file("plain-samples/plain-sample-jdbc") dependsOn library settings(jdbcSettings: _*) settings(assemblySettings: _*)

lazy val servlet = project in file("plain-samples/plain-sample-servlet") dependsOn library settings(assemblySettings: _*)

lazy val benchmark = project in file("plain-benchmark") dependsOn library settings(jdbcSettings: _*) settings(assemblySettings: _*)

lazy val integration = project in file("plain-integration") dependsOn library settings(camelSettings ++ graphSettings: _*)

lazy val integrationhelloworld = project in file("plain-samples/plain-integration-hello-world") dependsOn integration settings(assemblySettings ++ runSettings ++ scalariformSettings ++ graphSettings: _*)

lazy val preparation = project in file("plain-samples/preparation") dependsOn library settings(assemblySettings: _*)


