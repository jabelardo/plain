package com.ibm.plain

package lib

package logging

/**
 * Mixin this trait to get a protected member log: akka.logging.LoggingAdapter.
 */
trait HasLogger {

  protected[this] val log = createLogger(this)

}

