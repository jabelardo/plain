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
  val reflections = "org.reflections" % "reflections" % "0.9.8"
  val junit = "junit" % "junit" % "4.11"
  val junitItf = "com.novocode" % "junit-interface" % "0.10"
  val config = "com.typesafe" % "config" % "1.2.0"
  val slf4j = "org.slf4j" % "slf4j-api" % "1.7.6"
  val log4j2slf4j = "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.0-rc1"
  val log4j2api = "org.apache.logging.log4j" % "log4j-api" % "2.0-rc1"
  val log4j2core = "org.apache.logging.log4j" % "log4j-core" % "2.0-rc1"
  val disruptor = "com.lmax" % "disruptor" % "3.2.0"
  val commonsLang = "org.apache.commons" % "commons-lang3" % "3.2.1"
  val commonsComp = "org.apache.commons " % "commons-compress" % "1.7"
  val commonsIo = "commons-io" % "commons-io" % "2.4"
  val commonsNet = "commons-net" % "commons-net" % "3.3"
  val commonsCodec = "commons-codec" % "commons-codec" % "1.9"
  val clHashMap = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4"
  val lz4 = "net.jpountz.lz4" % "lz4" % "1.2.0"
  val zip4j = "net.lingala.zip4j" % "zip4j" % "1.3.1"
  val fasterXml = "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.0"
  val jerseyJson = "com.sun.jersey" % "jersey-json" % "1.18"
  val derbyjdbc = "org.apache.derby" % "derby" % "10.10.1.1"
  val derbyclient = "org.apache.derby" % "derbyclient" % "10.10.1.1"
  val h2jdbc = "com.h2database" % "h2" % "1.3.174"
  val mysqljdbc = "mysql" % "mysql-connector-java" % "5.1.29"
  val servlet31 = "javax.servlet" % "javax.servlet-api" % "3.1.0"
  val jsp23 = "org.glassfish.web" % "javax.servlet.jsp" % "2.3.2"
  val jstl12 = "org.glassfish.web" % "javax.servlet.jsp.jstl" % "1.2.2" 

  // Commercial JDBC drivers must be provided at runtime, sorry. Here we list what we have tested.
  val oraclejdbc = "com.oracle" % "ojdbc" % "11.2.0"
  val sqlsvrjdbc = "com.microsoft.sqlserver" % "sqljdbc4" % "4.0"

}

