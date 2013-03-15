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

    http.startupServers.foreach(path ⇒ appl.register(http.Server(path, Some(appl), None, None)))
    jdbc.startupConnectionFactories.foreach(path ⇒ appl.register(jdbc.ConnectionFactory(path)))

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
   * Must match the version string provided by the *.conf files.
   */
  final val requiredVersion = "1.0.1"

  final val home = getString("plain.home", System.getenv("PLAIN_HOME"))

  /**
   * check requirements
   */
  require(null != home, "Neither plain.home config setting nor PLAIN_HOME environment variable are set.")
  require(requiredVersion == config.version, String.format("plain.version in *.conf files (%s) does not match internal version (%s).", config.version, requiredVersion))

}

