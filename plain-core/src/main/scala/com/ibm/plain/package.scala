package com.ibm

import scala.concurrent.duration.Duration

package object plain

    extends config.CheckedConfig {

  import config._
  import config.settings._

  import bootstrap.Application
  import concurrent.Concurrent
  import logging.Logging
  import time.Time
  import io.Io
  import aio.Aio
  import monitor.extension.jmx.JmxMonitor
  import servlet.ServletContainer

  final def run: Unit = run(())

  final def run(body: ⇒ Unit): Unit = run(Duration.fromNanos(Long.MaxValue))(body)

  final def run(timeout: Duration)(body: ⇒ Unit): Unit = {
    /**
     * This is the central point for registering Components to the application in the correct order.
     */
    val application = {

      val appl = Application.instance
        .register(Concurrent.instance)
        .register(Logging.instance)
        .register(Time.instance)
        .register(Io.instance)
        .register(Aio.instance)
        .register(JmxMonitor.instance)
        .register(ServletContainer.instance)

      jdbc.startupConnectionFactories.foreach(path ⇒ appl.register(jdbc.ConnectionFactory(path)))

      http.startupServers.foreach(path ⇒ appl.register(http.Server(path, Some(appl), None, None)))

      appl
    }
    try {
      application.bootstrap
      if (!os.hostResolved) logging.createLogger(this).warn("Hostname not yet resolved, maybe some DNS problem.")
      body
      application.awaitTermination(timeout)
    } catch {
      case e: Throwable ⇒ e.printStackTrace; println("Exception during bootstrap, program will be aborted : " + e)
    } finally {
      try {
        application.teardown
      } catch {
        case e: Throwable ⇒ println("Exception during teardown : " + e)
      }
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

  /**
   * Careful: This is very expensive.
   */
  final def getStackDepth: Int = {
    val mxBean = java.lang.management.ManagementFactory.getThreadMXBean
    val threadInfo = mxBean.getThreadInfo(Thread.currentThread.getId, Int.MaxValue)
    threadInfo.getStackTrace.length
  }

  final def unsupported = throw new UnsupportedOperationException(getCallingFunctionName(6))

  final def unsupported(b: Boolean) = throw unsupported_

  final def nyi = throw notyetimplemented_

  final def notyetimplemented = throw notyetimplemented_

  final def deprecated = throw deprecated_

  final def ignore(b: ⇒ Any) = try b catch { case e: Throwable ⇒ }

  final def ignoreOrElse[A](b: ⇒ A, failvalue: A) = try b catch { case e: Throwable ⇒ failvalue }

  final def dumpStack = try { throw new Exception(getCallingFunctionName(6)) } catch { case e: Throwable ⇒ e.printStackTrace }

  final def dump[A](b: ⇒ A) = try b catch { case e: Throwable ⇒ println(e); throw e }

  final def try2Boolean(p: ⇒ Any, show: Boolean = false) = try { p; true } catch { case e: Throwable ⇒ if (show) e.printStackTrace; false }

  private[this] final val unsupported_ = new UnsupportedOperationException

  private[this] final val notyetimplemented_ = new UnsupportedOperationException("Not yet implemented.")

  private[this] final val deprecated_ = new UnsupportedOperationException("Deprecated.")

}

