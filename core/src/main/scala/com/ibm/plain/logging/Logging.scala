package com.ibm

package plain

package logging

import scala.collection.JavaConversions._

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.Configurator
import org.slf4j.{ Logger ⇒ JLogger, LoggerFactory }

import bootstrap.{ Application, BaseComponent, IsSingleton, Singleton }

/**
 *
 */
final class Logging private

    extends BaseComponent[Logging]("plain-logging")

    with IsSingleton {

  override final def start = {
    defaultLogger.trace("Logging started.")
    defaultLogger.debug("Components dependency graph (and bootstrap order) : " + Application.instance.getComponents(classOf[BaseComponent[_]]).filter(_.isEnabled).map(_.name).mkString(" ⇒ "))
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
 * The Logging singleton.
 */
object Logging

  extends Singleton[Logging](new Logging)

