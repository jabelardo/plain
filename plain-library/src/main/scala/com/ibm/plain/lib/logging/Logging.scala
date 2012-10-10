package com.ibm.plain

package lib

package logging

import java.io.{ FileOutputStream, PrintStream }
import java.nio.file.{ Files, Paths }

import scala.concurrent.util.Duration

import org.slf4j.LoggerFactory

import akka.actor.ActorSystem
import akka.event.{ Logging ⇒ AkkaLogging }
import akka.event.Logging.{ DebugLevel, ErrorLevel, InfoLevel, WarningLevel, levelFor }
import akka.event.LoggingAdapter
import bootstrap.BaseComponent
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.util.StatusPrinter

import bootstrap.BaseComponent
import concurrent.sleep

/**
 * Just needed for inheritance.
 */
abstract sealed class Logging

  extends BaseComponent[Logging]("plain-logging") {

  def isStarted = !isStopped

  def isStopped = loggingSystem.isTerminated

  def start = {
    if (isEnabled) {
      if (isStopped) throw new IllegalStateException("Underlying system already terminated.")
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

      if (config.logConfigOnStart) if (defaultLogger.isInfoEnabled) defaultLogger.info(config.settings.root.render)
    }
    this
  }

  def stop = {
    if (isStarted) {
      loggingSystem.shutdown
      sleep(200)
    }
    this
  }

  def awaitTermination(timeout: Duration) = if (!loggingSystem.isTerminated) loggingSystem.awaitTermination(timeout)

  def infoLevel = loggingSystem.eventStream.setLogLevel(InfoLevel)

  def createLogger(any: Any): LoggingAdapter = AkkaLogging.getLogger(loggingSystem.eventStream, any.getClass)

  def createLogger(name: String): LoggingAdapter = AkkaLogging.getLogger(loggingSystem.eventStream, name)

  def getLogLevel = loggingSystem.eventStream.logLevel match {
    case DebugLevel ⇒ "Debug"
    case InfoLevel ⇒ "Info"
    case WarningLevel ⇒ "Warning"
    case ErrorLevel ⇒ "Error"
    case l ⇒ l.toString
  }

  def setLogLevel(level: String) = loggingSystem.eventStream.setLogLevel(level match {
    case "Debug" ⇒ DebugLevel
    case "Info" ⇒ InfoLevel
    case "Warning" ⇒ WarningLevel
    case "Error" ⇒ ErrorLevel
    case _ ⇒ loggingSystem.eventStream.logLevel
  })

  private[this] final lazy val loggingSystem = {
    val system = ActorSystem(name)
    system.eventStream.setLogLevel(levelFor(loggingLevel).getOrElse(DebugLevel))
    system
  }

}

/**
 * The Logging object.
 */
object Logging extends Logging

