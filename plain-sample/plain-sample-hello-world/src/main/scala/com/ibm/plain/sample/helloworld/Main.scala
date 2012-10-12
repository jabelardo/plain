package com.ibm.plain.sample.helloworld

import scala.concurrent.util.duration.intToDurationInt

import com.ibm.plain.lib.{ requiredversion, run, application }
import com.ibm.plain.lib.bootstrap.Application
import com.ibm.plain.lib.concurrent.{ Concurrent, schedule }
import com.ibm.plain.lib.http.HttpServer
import com.ibm.plain.lib.io.{ temporaryDirectory, temporaryFile }
import com.ibm.plain.lib.json.jsonparser
import com.ibm.plain.lib.logging.defaultLogger.{ debug, error, info, warning }
import com.ibm.plain.lib.os.{ hostname, operatingSystem, username }

/**
 * Basic testing of plain-library in a stand-alone executable jar application.
 */
object Main extends App {

  application.register(HttpServer(5757, 0))

  run(45.seconds) {

    println("Application " + application)
    println("executor " + Concurrent.executor)
    println(requiredversion)
    println(operatingSystem)
    println(username)
    println(hostname)
    jsonparser("""{"name":"value"}""")
    val f = temporaryFile
    val d = temporaryDirectory
    println(f)
    println(d)
    debug("debug")
    info("info")
    warning("warning")
    error("error")
    var c = 0
    //    schedule(100, 1000) {
    //      c += 1
    //      println("print " + c)
    //      debug("debug " + c)
    //      info("info " + c)
    //      warning("warning " + c)
    //      error("error " + c)
    //    }
    println("before end of run")
  }
  println("after plain")

}

