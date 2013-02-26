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

  val scalareflect  = "org.scala-lang"                          %   "scala-reflect"               % "2.10.0"
  val junit         = "junit"                                   %   "junit"                       % "4.10"
  val junitItf      = "com.novocode"                            %   "junit-interface"             % "0.8"
  val config        = "com.typesafe"                            %   "config"                      % "1.0.0"
  val logback       = "ch.qos.logback"                          %   "logback-classic"             % "1.0.9"
  val janino        = "org.codehaus.janino"                     %   "janino"                      % "2.6.1"
  val akkaActor     = "com.typesafe.akka"                       %%  "akka-actor"                  % "2.1.1"
  val akkaTestKit   = "com.typesafe.akka"                       %%  "akka-testkit"                % "2.1.1"
  val akkaSlf4j     = "com.typesafe.akka"                       %%  "akka-slf4j"                  % "2.1.1"
  val commonsLang   = "org.apache.commons"                      %   "commons-lang3"               % "3.1"
  val commonsComp   = "org.apache.commons "                     %   "commons-compress"            % "1.4.1"
  val commonsIo     = "commons-io"                              %   "commons-io"                  % "2.4"
  val commonsNet    = "commons-net"                             %   "commons-net"                 % "3.1"
  val commonsCodec  = "commons-codec"                           %   "commons-codec"               % "1.7"
  val reflections   = "org.reflections"                         %   "reflections"                 % "0.9.8"
  val clHashMap     = "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.3.2"
  val jacksonCore   = "org.codehaus.jackson"                    %   "jackson-core-asl"            % "1.9.11"
  val jacksonMapper = "org.codehaus.jackson"                    %   "jackson-mapper-asl"          % "1.9.11"
  val fasterXml     = "com.fasterxml.jackson.core"              %   "jackson-databind"            % "2.1.1"
  val jerseyJson    = "com.sun.jersey"                          %   "jersey-json"                 % "1.16"
  val mimepull      = "org.jvnet.mimepull"                      %   "mimepull"                    % "1.8"
  val shapeless     = "com.chuusai"                             %%  "shapeless"                   % "1.2.2"
  val scalatest     = "org.scalatest"                           %%  "scalatest"                   % "1.9.1"
  val specs2        = "org.specs2"                              %%  "specs2"                      % "1.12.3"

}

