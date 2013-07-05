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

  def compile     (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided    (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test        (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def testDefault (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test->default")
  def runtime     (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container   (deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  val scalareflect  = "org.scala-lang"                          %   "scala-reflect"               % "2.10.2"
  val scalatest     = "org.scalatest"                           %%  "scalatest"                   % "1.9.1"
  val specs2        = "org.specs2"                              %%  "specs2"                      % "1.14"
  val junit         = "junit"                                   %   "junit"                       % "4.11"
  val junitItf      = "com.novocode"                            %   "junit-interface"             % "0.9"
  val config        = "com.typesafe"                            %   "config"                      % "1.0.1"
  val slf4jnoop     = "org.slf4j"                               %   "slf4j-nop"                   % "1.6.4"
  val logback       = "ch.qos.logback"                          %   "logback-classic"             % "1.0.12"
  val janino        = "org.codehaus.janino"                     %   "janino"                      % "2.6.1"
  val akkaActor     = "com.typesafe.akka"                       %%  "akka-actor"                  % "2.1.4"
  val akkaTestKit   = "com.typesafe.akka"                       %%  "akka-testkit"                % "2.1.4"
  val akkaSlf4j     = "com.typesafe.akka"                       %%  "akka-slf4j"                  % "2.1.4"
  val shapeless     = "com.chuusai"                             %%  "shapeless"                   % "1.2.4"
  val commonsLang   = "org.apache.commons"                      %   "commons-lang3"               % "3.1"
  val commonsComp   = "org.apache.commons "                     %   "commons-compress"            % "1.5"
  val commonsIo     = "commons-io"                              %   "commons-io"                  % "2.4"
  val commonsNet    = "commons-net"                             %   "commons-net"                 % "3.3"
  val commonsCodec  = "commons-codec"                           %   "commons-codec"               % "1.8"
  val reflections   = "org.reflections"                         %   "reflections"                 % "0.9.8"
  val clHashMap     = "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.3.2"
  val lz4           = "net.jpountz.lz4"                         %   "lz4"                         % "1.1.2"
  val fasterXml     = "com.fasterxml.jackson.core"              %   "jackson-databind"            % "2.2.2"
  val jerseyJson    = "com.sun.jersey"                          %   "jersey-json"                 % "1.17.1"
  val mimepull      = "org.jvnet.mimepull"                      %   "mimepull"                    % "1.9.3"
  val derbyjdbc     = "org.apache.derby"                        %   "derby"                       % "10.10.1.1"
  val derbyclient   = "org.apache.derby"                        %   "derbyclient"                 % "10.10.1.1"
  val h2jdbc        = "com.h2database"                          %   "h2"                          % "1.3.172"
  val mysqljdbc     = "mysql"                                   %   "mysql-connector-java"        % "5.1.25"
  val servlet25     = "javax.servlet"                           %   "servlet-api"                 % "2.5"
  val disruptor     = "com.lmax"                                %   "disruptor"                   % "3.1.0"

  /**
   * Commercial JDBC drivers must be provided at runtime, sorry. Here we list what we have tested.
   */
  // val oraclejdbc    = "com.oracle"                              %   "ojdbc"                       % "11.2.0"
  // val db2jdbc       = "com.ibm"                                 %   ""                            % ""
  // val sqlsvrjdbc    = "com.microsoft.sqlserver"                 %   "sqljdbc4"				     % "4.0"
  
}

