import sbt._
import Keys._

scalariformSettings

name := "plain.io"

organization in ThisBuild := "com.ibm"

scalaVersion in ThisBuild := "2.10.3"

version in ThisBuild := "1.0.0-SNAPSHOT"

mainClass in ThisBuild := Some("com.ibm.plain.bootstrap.Main")

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

