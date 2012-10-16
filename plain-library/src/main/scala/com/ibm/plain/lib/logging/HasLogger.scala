package com.ibm.plain

package lib

package logging

/**
 * Mixin this trait to get a protected implicit member log: akka.logging.LoggingAdapter.
 */
trait HasLogger {

  protected final def debug(s: String) = log.debug(s)

  protected final def info(s: String) = log.info(s)

  protected final def warning(s: String) = log.warning(s)

  protected final def error(s: String) = log.error(s)

  implicit protected final val log = createLogger(this)

}

