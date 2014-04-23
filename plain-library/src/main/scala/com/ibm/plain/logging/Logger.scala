package com.ibm

package plain

package logging

import org.slf4j.{ Logger ⇒ JLogger }
import org.apache.logging.log4j.LogManager

/**
 *
 */
trait Logger {

  final def trace(message: ⇒ String): Unit = if (logger.isTraceEnabled) logger.trace(message)

  final def debug(message: ⇒ String): Unit = if (logger.isDebugEnabled) logger.debug(message)

  final def info(message: ⇒ String) = if (logger.isInfoEnabled) logger.info(message)

  final def warn(message: ⇒ String) = if (logger.isErrorEnabled) logger.warn(message)

  final def error(message: ⇒ String) = if (logger.isErrorEnabled) logger.error(message)

  protected final def logger: JLogger = { if (null == jlogger) jlogger = Logging.instance.createJLogger(loggername); jlogger }

  protected val loggername: String = getClass.getName.replace("$", "")

  private[this] final var jlogger: JLogger = null

}

/**
 *
 */
final class NamedLogger(

  override protected final val loggername: String)

  extends Logger
