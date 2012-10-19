package com.ibm.plain.sample.helloworld

import language.postfixOps
import scala.concurrent.util.duration.intToDurationInt

import com.ibm.plain.lib.run

/**
 * The most simple Main application, but there is really nothing more to do (it's all in the application.conf).
 */
object Main extends App {

  run(5 minutes) {}

  println("Good bye.")

}

