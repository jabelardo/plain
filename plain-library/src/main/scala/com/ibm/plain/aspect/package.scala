package com.ibm

package plain

import config.CheckedConfig
import logging.HasLogger

package object aspect

  extends CheckedConfig

  with HasLogger {

  import config._
  import config.settings._

}
