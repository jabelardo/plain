package com.ibm.plain

package lib

package object bootstrap

  extends config.CheckedConfig {

  final lazy val application = Application

  /**
   * Call this from anywhere in order to terminate the jvm with a message and a given exit code.
   */
  def terminateJvm(reason: Throwable, code: Int, stacktrace: Boolean = false): Nothing = try {
    if (stacktrace) reason.printStackTrace
    val message = """
plain-library : %s
plain-library : Memory free/max/total : %d %d %d
plain-library : Program will abort now"""
    val runtime = Runtime.getRuntime
    println(message.format(reason, runtime.freeMemory, runtime.maxMemory, runtime.totalMemory))
    runtime.exit(code)
    throw reason
  } catch {
    case e: Throwable ⇒
      Runtime.getRuntime.exit(code)
      throw reason
  }

}
