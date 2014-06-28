package com.ibm

package plain

import config.{ CheckedConfig, config2RichConfig }

package object monitor

  extends CheckedConfig {

  import config._
  import config.settings._

  final val shutdownToken = getString("plain.monitor.shutdown-token", "1234")

}

