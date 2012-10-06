package com.ibm.plain.sample.helloworld

import com.ibm.plain.lib.concurrent._
import com.ibm.plain.lib.logging.log._

/**
 * Basic testing of plain-library in a stand-alone executable jar application.
 */
object Main extends App {

  println("starting main")
  println(actorSystem)
  debug("debug")
  info("info")
  warning("warning")
  error("error")
  spawn { sleep(2500); shutdown }
  awaitTermination
  println("main finished")

}