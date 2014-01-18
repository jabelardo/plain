package com.ibm

import scala.concurrent.duration.Duration
import scala.concurrent.duration.TimeUnit

package object plain

  extends config.CheckedConfig {

  import config._
  import config.settings._
  import concurrent.scheduleGcTimeout

  /**
   * This is the central point for registering Components to the application in the correct order.
   */
  final lazy val application = {

    val appl = bootstrap.application
      .register(concurrent.Concurrent)
      .register(logging.Logging)
      .register(io.Io)
      .register(aio.Aio)
      .register(monitor.extension.jmx.JmxMonitor)

    jdbc.startupConnectionFactories.foreach(path ⇒ appl.register(jdbc.ConnectionFactory(path)))

    http.startupServers.foreach(path ⇒ appl.register(http.Server(path, Some(appl), None, None)))

    appl.register(servlet.ServletContainer)

    appl
  }

  def run: Unit = run(())

  def run(body: ⇒ Unit): Unit = run(Duration.fromNanos(Long.MaxValue))(body)

  def run(timeout: Duration)(body: ⇒ Unit): Unit = try {
    application.bootstrap
    if (0 < scheduleGcTimeout) concurrent.schedule(scheduleGcTimeout, scheduleGcTimeout) { sys.runtime.gc }
    body
    application.awaitTermination(timeout)
  } catch {
    case e: Throwable ⇒ e.printStackTrace; println("Exception during bootstrap : " + e)
  } finally {
    try {
      application.teardown
    } catch {
      case e: Throwable ⇒ println("Exception during teardown : " + e)
    }
  }

  /**
   * low-level code shorteners
   */
  final def getCallingFunctionName(depth: Int): String = {
    val mxBean = java.lang.management.ManagementFactory.getThreadMXBean
    val threadInfo = mxBean.getThreadInfo(Thread.currentThread.getId, depth)
    val elements = threadInfo.getStackTrace
    elements(depth - 1).getClassName + "." + elements(depth - 1).getMethodName
  }

  final def unsupported = throw new UnsupportedOperationException(getCallingFunctionName(6))

  final def unsupported(b: Boolean) = throw unsupported_

  final def nyi = throw notyetimplemented_

  final def notyetimplemented = throw notyetimplemented_

  final def deprecated = throw deprecated_

  final def ignore(b: ⇒ Any) = try b catch { case e: Throwable ⇒ }

  final def ignoreOrElse[A](b: ⇒ A, failvalue: A) = try b catch { case e: Throwable ⇒ failvalue }

  private[this] final val unsupported_ = new UnsupportedOperationException

  private[this] final val notyetimplemented_ = new UnsupportedOperationException("Not yet implemented.")

  private[this] final val deprecated_ = new UnsupportedOperationException("Deprecated.")

}

