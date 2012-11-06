package com.ibm

package plain

package logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply

class NameFilter extends Filter[ILoggingEvent] {

  override def decide(event: ILoggingEvent): FilterReply = {
    if (Level.DEBUG_INT >= event.getLevel.toInt) {
      filterDebugNames.foreach { loggername ⇒
        if (event.getLoggerName.contains(loggername)) {
          return FilterReply.DENY
        }
      }
    }
    FilterReply.NEUTRAL
  }

}

