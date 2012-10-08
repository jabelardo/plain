package com.ibm.plain

package lib

import config.CheckedConfig

package object monitor

  extends CheckedConfig {

  import config._
  import config.settings._

  /**
   * defaults to monitor.extension.jmx
   */
  def register = extension.jmx.register

  final val shutdownToken = getString("plain.monitor.shutdown-token", "1234")

}

