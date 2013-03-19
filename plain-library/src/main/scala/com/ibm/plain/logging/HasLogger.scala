package com.ibm

package plain

package logging

import akka.event.{ BusLogging, EventStream }

/**
 * Mixin this trait to get a protected implicit member log: akka.logging.LoggingAdapter.
 */
trait HasLogger {

  final def disableLogging = log_ = new BusLogging(new EventStream(false), "", this.getClass)

  final def enableLogging = log_ = createLogger(this)

  protected final def debug(s: String) = log.debug(s)

  protected final def info(s: String) = log.info(s)

  protected final def warning(s: String) = log.warning(s)

  protected final def error(s: String) = log.error(s)

  implicit protected final def log = log_

  @volatile private[this] final var log_ = createLogger(this)

}

