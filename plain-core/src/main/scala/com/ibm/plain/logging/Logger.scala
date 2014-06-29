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

  final def trace(message: Any): Unit = if (logger.isTraceEnabled) logger.trace(if (null == message) "null" else message.toString)

  final def debug(message: Any): Unit = if (logger.isDebugEnabled) logger.debug(if (null == message) "null" else message.toString)

  final def info(message: Any) = if (logger.isInfoEnabled) logger.info(if (null == message) "null" else if (null == message) "null" else message.toString)

  final def warn(message: Any) = if (logger.isErrorEnabled) logger.warn(if (null == message) "null" else message.toString)

  final def error(message: Any) = if (logger.isErrorEnabled) logger.error(if (null == message) "null" else message.toString)

  protected final def logger: JLogger = { if (null == jlogger) jlogger = Logging.instance.createJLogger(loggername); jlogger }

  protected val loggername: String = getClass.getName.replace("$", "")

  private[this] final var jlogger: JLogger = null

  private[this] final def check(a: Any) = ""

}

/**
 *
 */
final class NamedLogger(

  override protected final val loggername: String)

    extends Logger
