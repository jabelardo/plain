package com.ibm

package plain

import scala.language.implicitConversions

package object bootstrap

    extends config.CheckedConfig {

  import config._
  import config.settings._

  final val delayDuringTeardown = getMilliseconds("plain.bootstrap.delay-during-teardown", 20)

  final val disableApplicationExtensions = getBoolean("plain.bootstrap.disable-application-extensions", false)

  /**
   * Call this from anywhere in order to terminate the jvm with a message and a given exit code.
   */
  def terminateJvm(reason: Throwable, code: Int, stacktrace: Boolean = false): Nothing = try {
    if (stacktrace) reason.printStackTrace
    try Application.instance.teardown catch { case e: Throwable ⇒ System.err.println(e) }
    val message = """
Error : %s
Memory used/free/max/total (mb) : %d %d %d %d
Program will abort now."""
    val runtime = sys.runtime
    import runtime._
    def m(b: Long) = (b / (1024 * 1024)).toLong
    System.err.println(message.format(reason, m(maxMemory - freeMemory), m(freeMemory), m(maxMemory), m(totalMemory)))
    sys.exit(code)
  } catch {
    case e: Throwable ⇒
      sys.exit(code)
  }

}
