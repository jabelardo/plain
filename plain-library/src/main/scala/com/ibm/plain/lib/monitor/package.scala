package com.ibm.plain

package lib

import config.{ CheckedConfig, config2RichConfig }

package object monitor

  extends CheckedConfig {

  import config._
  import config.settings._

  final val shutdownToken = getString("plain.monitor.shutdown-token", "1234")

}

