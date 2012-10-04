package com.ibm.plain

package lib

import java.io.{ FileOutputStream, PrintStream }
import java.nio.file.{ Files, Paths }

import scala.collection.JavaConversions.asScalaBuffer
import scala.concurrent.util.Duration

import org.slf4j.LoggerFactory

import akka.actor.ActorSystem
import akka.event.{ Logging, LoggingAdapter }
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter

package object logging

  extends config.CheckedConfig {

  import config._
  import config.settings._

  def createLogger(any: Any) = Logging.getLogger(loggingSystem.eventStream, any.getClass)

  def shutdown = {
    loggingSystem.eventStream.setLogLevel(Logging.InfoLevel)
    loggingSystem.shutdown
  }

  def isTerminated = loggingSystem.isTerminated

  def awaitTermination(timeout: Duration) = loggingSystem.awaitTermination(timeout)

  def awaitTermination = loggingSystem.awaitTermination(Duration.Inf)

  final val logginglevel = {
    val level = getString("plain.logging.level").toUpperCase
    System.setProperty("rootLevel", level match {
      case "WARNING" ⇒ "WARN"
      case v ⇒ v
    })
    level
  }

  final val loggingSystem = {
    val system = ActorSystem("plain-logging")
    system.eventStream.setLogLevel(Logging.levelFor(logginglevel).getOrElse(Logging.DebugLevel))
    system
  }

  final val loggingConsole = new LogSettings("plain.logging.console")

  final val loggingText = new LogSettings("plain.logging.text")

  final val loggingHtml = new LogSettings("plain.logging.html")

  final val filterDebugLoggerNames: List[String] = try {
    getStringList("plain.logging.filter-debug-logger-names").toList
  } catch {
    case _: Throwable ⇒ List.empty
  }

  final class LogSettings(path: String) {

    private[this] val cfg = getConfig(path)

    final val enable = cfg.getBoolean("enable")

    System.setProperty(path + ".enable", enable.toString)

    final val toFile = if (enable) cfg.getString("file") match {
      case "" | "." | null ⇒ false
      case v ⇒
        System.setProperty(path + ".file", v)
        true
    }
    else false

    if (enable) cfg.getString("pattern") match {
      case "" | null ⇒
      case v ⇒ System.setProperty(path + ".pattern", v)
    }

    if (enable) cfg.getString("rolling-pattern") match {
      case "" | null ⇒
      case v ⇒ System.setProperty(path + ".rolling-pattern", v)
    }

  }

  final lazy val log = {
    val l = _log
    if (logConfigOnStart) l.info(root.render)
    l
  }

  private[this] lazy val _log: LoggingAdapter = {
    LoggerNameFilter.filterDebugLoggerNames = filterDebugLoggerNames
    val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    try {
      val configurator = new JoranConfigurator
      configurator.setContext(context)
      context.reset
      configurator.doConfigure(getClass.getClassLoader.getResourceAsStream("logback.xml"))
      if (loggingConsole.enable && loggingConsole.toFile) {
        try {
          val path = Paths.get(System.getProperty("plain.logging.console.file"))
          Files.createDirectories(path.getParent)
          val console = new PrintStream(new FileOutputStream(path.toFile))
          System.setOut(console)
          System.setErr(console)
        } catch {
          case e: Throwable ⇒
            println("Could not create plain.logging.console.file : " + e)
        }
      }
    } catch {
      case e: Throwable ⇒ println()
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context)
    Logging.getLogger(loggingSystem, loggingSystem.name)
  }

}
