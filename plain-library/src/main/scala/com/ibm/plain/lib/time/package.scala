package com.ibm.plain

package lib

import language.postfixOps
import scala.concurrent.util.duration.longToDurationLong

import akka.event.LoggingAdapter
import config.CheckedConfig

/**
 * Utilities to ease the handling of time values and simple measurement for profiling.
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
  def timeMillis[R](f: ⇒ R): (R, Long) = { val begin = now; val r = f; (r, now - begin) }

  /**
   * Executes f and prints the time elapsed in milliseconds.
   */
  def infoMillis[R](f: ⇒ R)(implicit log: LoggingAdapter): R = { val r = timeMillis(f); log.info((r._2 / 1000.0) + " sec"); r._1 }

  /**
   * Executes f and prints message and the time elapsed in nanoseconds.
   */
  def infoMillis[R](msg: String)(f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeMillis(f); log.info(msg + " " + (r._2 / 1000.0) + " sec"); r._1 }

  /**
   * Executes f and returns the time elapsed in nanoseconds.
   */
  def timeNanos[R](f: ⇒ R): (R, Long) = { val begin = nowNanos; val r = f; (r, nowNanos - begin) }

  /**
   * Executes f and prints the time elapsed in nanoseconds.
   */
  def infoNanos[R](f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info((r._2 / 1000000000.0) + " sec"); r._1 }

  /**
   * Executes f and prints message and the time elapsed in nanoseconds.
   */
  def infoNanos[R](msg: String)(f: ⇒ R)(implicit log: LoggingAdapter) = { val r = timeNanos(f); log.info(msg + " " + (r._2 / 1000000000.0) + " sec"); r._1 }

  /**
   * 0 seconds.
   */
  val never = 0L seconds

  /**
   * 1000 years.
   */
  val forever = 1000L * 365 days

}
