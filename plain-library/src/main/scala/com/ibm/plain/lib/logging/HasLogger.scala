package com.ibm.plain

package lib

package logging

/**
 * Mixin this trait to get a protected implicit member log: akka.logging.LoggingAdapter.
 */
trait HasLogger {

  protected[this] final def debug(s: String) = log.debug(s)

  protected[this] final def info(s: String) = log.info(s)

  protected[this] final def warning(s: String) = log.warning(s)

  protected[this] final def error(s: String) = log.error(s)

  protected[this] implicit final val log = createLogger(this)

}

