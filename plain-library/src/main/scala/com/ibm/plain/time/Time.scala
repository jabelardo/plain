package com.ibm

package plain

package time

import java.util.concurrent.ScheduledFuture

import bootstrap.BaseComponent
import concurrent.schedule

/**
 *
 */
abstract sealed class Time

  extends BaseComponent[Time]("plain-time") {

  override def start = {
    if (!isStarted) formattime = schedule(2000) { rfc1123bytearray = rfc1123format.format(now).getBytes }
    this
  }

  override def stop = {
    if (!isStopped) if (null != formattime) formattime.cancel(true)
    this
  }

  private[this] final var formattime: ScheduledFuture[_] = null
}

/**
 * The Time object.
 */
object Time extends Time

