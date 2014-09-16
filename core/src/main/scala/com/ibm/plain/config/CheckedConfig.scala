package com.ibm.plain
package config

import text.stackTraceToString
import logging.defaultLogger

/**
 *
 */
trait CheckedConfig

    extends DelayedInit {

  override def delayedInit(body: ⇒ Unit): Unit = {
    try body catch { case e: Throwable ⇒ handleError(e) }
  }

  def handleError(e: Throwable) = {
    ignore {
      defaultLogger.error("Configuration error : " + e)
      defaultLogger.trace(stackTraceToString(e))
    }
    if (rethrowExceptionOnError)
      throw e
    else if (terminateOnError)
      bootstrap.terminateJvm(e, terminateOnErrorExitCode, printStackTraceOnError)
    else if (printStackTraceOnError)
      e.printStackTrace
    else
      println(e)
  }

}
