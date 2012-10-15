package com.ibm.plain

package lib

package config

trait CheckedConfig

  extends DelayedInit {

  override def delayedInit(body: ⇒ Unit): Unit = {
    try {
      body
    } catch {
      case e: Throwable ⇒ handleError(e)
    }
  }

  def handleError(e: Throwable) = {
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

