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

  val junit         = "junit"                                   %   "junit"                       % "4.10"
  val junitItf      = "com.novocode"                            %   "junit-interface"             % "0.8"
  val config        = "com.typesafe"                            %   "config"                      % "0.5.2"
  val akkaActor     = "com.typesafe.akka"                       %   "akka-actor_2.10.0-M7"        % "2.1-M2"
  val akkaTestKit   = "com.typesafe.akka"                       %   "akka-testkit_2.10.0-M7"      % "2.1-M2"
  val akkaSlf4j     = "com.typesafe.akka"                       %   "akka-slf4j_2.10.0-M7"        % "2.1-M2"
  val clHashMap     = "com.googlecode.concurrentlinkedhashmap"  %   "concurrentlinkedhashmap-lru" % "1.3.1"
  val jettyWebApp   = "org.eclipse.jetty"                       %   "jetty-webapp"                % "8.1.5.v20120716"
  val servlet30     = "org.eclipse.jetty.orbit"                 %   "javax.servlet"               % "3.0.0.v201112011016" artifacts Artifact("javax.servlet", "jar", "jar")
  val liftJson      = "net.liftweb"                             %   "lift-json_2.9.1"             % "2.4"
  val logback       = "ch.qos.logback"                          %   "logback-classic"             % "1.0.4"
  val mimepull      = "org.jvnet.mimepull"                      %   "mimepull"                    % "1.8"
  val parboiled     = "org.parboiled"                           %%  "parboiled-scala"             % "1.1.1"
  val pegdown       = "org.pegdown"                             %   "pegdown"                     % "1.1.0"
  val scalate       = "org.fusesource.scalate"                  %   "scalate-core"                % "1.5.3"
  val shapeless     = "com.chuusai"                             %%  "shapeless"                   % "1.2.2"
  val scalatest     = "org.scalatest"                           %%  "scalatest"                   % "1.8"
  val specs2        = "org.specs2"                              %%  "specs2"                      % "1.11"
  val sprayJson     = "cc.spray"                                %%  "spray-json"                  % "1.1.1"
  val twirlApi      = "cc.spray"                                %%  "twirl-api"                   % "0.5.4"

}
