package com.ibm

import com.lmax.disruptor.util.Util.getUnsafe

import scala.concurrent.duration.Duration
import scala.concurrent.duration.TimeUnit

package object plain

  extends config.CheckedConfig {

  import config._
  import config.settings._
  import concurrent.scheduleGcTimeout

  require(8 == getUnsafe.addressSize, "Sorry, but PLAIN only runs on a 64bit platform.")

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
  def unsupported = throw new UnsupportedOperationException

  def deprecated = throw new UnsupportedOperationException("Deprecated.")

  def ignore(b: ⇒ Any) = try b catch { case e: Throwable ⇒ }

}

