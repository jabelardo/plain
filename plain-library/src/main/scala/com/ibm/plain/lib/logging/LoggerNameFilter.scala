package com.ibm.plain

package lib

package logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.Level
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class LoggerNameFilter extends Filter[ILoggingEvent] {

  import LoggerNameFilter._

  override def decide(event: ILoggingEvent): FilterReply = {
    if (Level.DEBUG_INT >= event.getLevel.toInt) {
      filterDebugLoggerNames.foreach { loggername ⇒
        if (event.getLoggerName.contains(loggername)) {
          return FilterReply.DENY
        }
      }
    }
    FilterReply.NEUTRAL
  }

}

object LoggerNameFilter {

  var filterDebugLoggerNames: List[String] = null

}
