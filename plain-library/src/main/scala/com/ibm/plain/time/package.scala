package com.ibm

package plain

import language.postfixOps
import scala.concurrent.duration._

import akka.event.LoggingAdapter
import config.CheckedConfig

/**
 * Utilities to ease the handling of time values and simple tools for micro benchmarking and profiling.
 */
package object time

  extends CheckedConfig {

  /**
   * now in milliseconds since 1970
   */
  def now: Long = System.currentTimeMillis

  /**
   * now in nanoseconds since 1970
   */
  def nowNanos: Long = System.nanoTime

  /**
   * Executes f and returns the time elapsed in milliseconds.
   */
  @inline def timeMillis[R](f: ⇒ R): (R, Long) = { val begin = now; val r = f; (r, now - begin) }

  /**
   * Executes f and log.info the time elapsed in milliseconds.
   */
  @inline def infoMillis[R](f: ⇒ R)(implicit log: LoggingAdapter): R = { val r = timeMillis(f); log.info((r._2 / 1000.0) + " sec"); r._1 }

  /**
   * Executes f and log.info message and the time elapsed in nanoseconds.
   */
  @inline def infoMillis[R](msg: String)(f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeMillis(f); log.info(msg + " " + (r._2 / 1000.0) + " sec"); r._1 }

  /**
   * Executes f and returns the time elapsed in nanoseconds.
   */
  @inline def timeNanos[R](f: ⇒ R): (R, Long) = { val begin = nowNanos; val r = f; (r, nowNanos - begin) }

  /**
   * Executes f and log.info the time elapsed in nanoseconds.
   */
  @inline def infoNanos[R](f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info((r._2 / 1000000.0) + " msec"); r._1 }

  /**
   * Executes f and log.info message and the time elapsed in nanoseconds.
   */
  @inline def infoNanos[R](msg: String)(f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info(msg + " " + (r._2 / 1000000.0) + " msec"); r._1 }

  /**
   * Executes f and log.info the time elapsed in nanoseconds.
   */
  @inline def infoNanoNanos[R](f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info(r._2 + " nanos"); r._1 }

  /**
   * Executes f and log.info message and the time elapsed in nanoseconds.
   */
  @inline def infoNanoNanos[R](msg: String)(f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info(msg + " " + r._2 + " nanos"); r._1 }

  /**
   * 0 seconds.
   */
  final val never = 0L seconds

  /**
   * 291 years (upper limit for scala.concurrent.duration.Duration).
   */
  final val forever = (365 * 291) days

}
