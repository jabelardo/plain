package com.ibm.plain

import scala.concurrent.util.Duration

package object lib

  extends config.CheckedConfig {

  import config._
  import config.settings._
  import bootstrap._

  def run(body: ⇒ Unit): Unit = run(Duration.Inf)(body)

  /**
   * This is the central point for registering Components to the Application in the correct order.
   */
  def run(timeout: Duration)(body: ⇒ Unit): Unit = try {
    application
      .register(logging.Logging)
      .register(concurrent.Concurrent)
      .register(monitor.extension.jmx.JmxMonitor)
      .register(http.HttpServer(http.port, http.backlog))
      .bootstrap
    body
    application.awaitTermination(timeout)
  } catch {
    case e: Throwable ⇒ println("Uncaught exception: " + e)
  } finally {
    try {
      application.teardown
    } catch {
      case e: Throwable ⇒ println("Exception during teardown: " + e)
    }
  }

  override lazy val toString = root.render

  /**
   * Must match the version string provided by the *.conf files.
   */
  final val requiredversion = "1.0.1"

  final val home = getString("plain.home", System.getenv("PLAIN_HOME"))

  /**
   * check requirements
   */
  require(null != home, "Neither plain.home config setting nor PLAIN_HOME environment variable are set.")
  require(requiredversion == config.version, String.format("plain.version (%s) does not match internal version (%s).", config.version, requiredversion))

}

