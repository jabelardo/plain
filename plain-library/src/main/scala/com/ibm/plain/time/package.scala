package com.ibm

package plain

import language.postfixOps
import scala.concurrent.duration._

import akka.event.LoggingAdapter
import config.CheckedConfig
import concurrent.schedule

/**
 * Utilities to ease the handling of time values and simple tools for micro benchmarking and profiling.
 */
package object time

  extends CheckedConfig {

  /**
   * now in milliseconds since 1970
   */
  final def now: Long = System.currentTimeMillis

  /**
   * now in nanoseconds since 1970
   */
  final def nowNanos: Long = System.nanoTime

  final val `UTC` = java.util.TimeZone.getTimeZone("UTC")

  final val rfc1123format = {
    val f = new java.text.SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US)
    f.setTimeZone(`UTC`)
    f
  }

  @volatile final var rfc1123bytearray: Array[Byte] = {
    schedule(1000, 2000) { rfc1123bytearray = rfc1123format.format(now).getBytes }
    null
  }

  // prevent "false sharing"
  @volatile final var padding1: Array[Byte] = null
  @volatile final var padding2: Array[Byte] = null
  @volatile final var padding3: Array[Byte] = null
  @volatile final var padding4: Array[Byte] = null
  @volatile final var padding5: Array[Byte] = null
  @volatile final var padding6: Array[Byte] = null

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
