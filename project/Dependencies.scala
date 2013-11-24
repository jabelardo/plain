/**
 *  Copyright (C) 2012 IBM
 */
package plain

import sbt._
import Keys._

/**
 * All dependencies to 3rd party libraries.
 */
object Dependencies {

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def testDefault(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test->default")
  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val scalareflect = "org.scala-lang" % "scala-reflect" % "2.10.3"
  val scalatest = "org.scalatest" %% "scalatest" % "1.9.1"
  val specs2 = "org.specs2" %% "specs2" % "2.11"
  val junit = "junit" % "junit" % "4.11"
  val junitItf = "com.novocode" % "junit-interface" % "0.10"
  val config = "com.typesafe" % "config" % "1.0.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.13"
  val janino = "org.codehaus.janino" % "janino" % "2.6.1"
  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.2.3"
  val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % "2.2.3"
  val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % "2.2.3"
  val commonsLang = "org.apache.commons" % "commons-lang3" % "3.1"
  val commonsComp = "org.apache.commons " % "commons-compress" % "1.5"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val commonsNet = "commons-net" % "commons-net" % "3.3"
  val commonsCodec = "commons-codec" % "commons-codec" % "1.8"
  val reflections = "org.reflections" % "reflections" % "0.9.8"
  val clHashMap = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4"
  val lz4 = "net.jpountz.lz4" % "lz4" % "1.2.0"
  val zip4j = "net.lingala.zip4j" % "zip4j" % "1.3.1"
  val fasterXml = "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.3"
  val jerseyJson = "com.sun.jersey" % "jersey-json" % "1.17.1"
  val mimepull = "org.jvnet.mimepull" % "mimepull" % "1.9.3"
  val derbyjdbc = "org.apache.derby" % "derby" % "10.10.1.1"
  val derbyclient = "org.apache.derby" % "derbyclient" % "10.10.1.1"
  val h2jdbc = "com.h2database" % "h2" % "1.3.173"
  val mysqljdbc = "mysql" % "mysql-connector-java" % "5.1.26"
  val servlet31 = "javax.servlet" % "javax.servlet-api" % "3.1.0"
  val jsp23 = "org.glassfish.web" % "javax.servlet.jsp" % "2.3.1"
  val disruptor = "com.lmax" % "disruptor" % "3.2.0"

  // Commercial JDBC drivers must be provided at runtime, sorry. Here we list what we have tested.
  val oraclejdbc = "com.oracle" % "ojdbc" % "11.2.0"
  val sqlsvrjdbc = "com.microsoft.sqlserver" % "sqljdbc4" % "4.0"

}

