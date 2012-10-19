package com.ibm.plain.sample.helloworld

import scala.concurrent.util.duration.intToDurationInt

import com.ibm.plain.lib.config.{ config2RichConfig, settings }
import com.ibm.plain.lib.run

/**
 * The most simple Main application, but there is really nothing more to do (it's all in the application.conf).
 */
object Main extends App {

  run(settings.getDuration("how-long-to-run")) {}

}

