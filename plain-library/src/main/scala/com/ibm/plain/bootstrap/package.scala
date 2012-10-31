package com.ibm

package plain

package object bootstrap

  extends config.CheckedConfig {

  final lazy val application = Application

  /**
   * Call this from anywhere in order to terminate the jvm with a message and a given exit code.
   */
  def terminateJvm(reason: Throwable, code: Int, stacktrace: Boolean = false): Nothing = try {
    if (stacktrace) reason.printStackTrace
    try application.teardown catch { case e: Throwable ⇒ println(e) }
    val message = """
%s
Memory used/free/max/total (mb) : %d %d %d %d
Program will abort now."""
    val runtime = Runtime.getRuntime
    import runtime._
    def m(b: Long) = (b / (1024 * 1024)).toLong
    println(message.format(reason, m(maxMemory - freeMemory), m(freeMemory), m(maxMemory), m(totalMemory)))
    exit(code)
    throw reason
  } catch {
    case e: Throwable ⇒
      Runtime.getRuntime.exit(code)
      throw reason
  }

}
