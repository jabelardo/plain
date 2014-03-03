package com.ibm

package plain

package logging

import scala.collection.JavaConversions._

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.slf4j.{ Logger ⇒ JLogger, LoggerFactory }

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Logging

  extends BaseComponent[Logging]("plain-logging") {

  override final def start = {
    defaultLogger.trace("Logging started.")
    this
  }

  override final def stop = {
    if (isStarted) try {
      defaultLogger.trace("Logging stopped.")
      Configurator.shutdown(LogManager.getContext(false).asInstanceOf[LoggerContext])
    } catch {
      case e: Throwable ⇒ defaultLogger.error("Logging shutdown failed : " + e)
    }
    this
  }

  final def getLevel = loggingLevel

  final def setLevel(level: String) = {
    val oldlevel = loggingLevel
    val newlevel = Level.toLevel(canonicalLevel(level))
    require(newlevel.name == canonicalLevel(level), "Invalid level. Valid values are ALL, TRACE, DEBUG, INFO, WARN, ERROR and OFF.")
    val context = LogManager.getContext(false).asInstanceOf[LoggerContext]
    context.getConfiguration.getLoggers.values.foreach(config ⇒ config.setLevel(newlevel))
    context.updateLoggers
    defaultLogger.info("Changed logging level from '" + oldlevel + "' to '" + newlevel + "'.")
  }

  def createLogger(any: Any): Logger = any match {
    case name: String ⇒ new NamedLogger(name)
    case any ⇒ new NamedLogger(any.getClass.getName.replace("$", ""))
  }

  private[logging] final def createJLogger(name: String): JLogger = LoggerFactory.getLogger(name)

}

/**
 * The Logging object.
 */
object Logging extends Logging

