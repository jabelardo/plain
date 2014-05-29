package com.ibm

package plain

package integration

package client

import bootstrap.{ Application, ApplicationExtension }

/**
 * Do some tests, then shutdown.
 */
final class ClientTester

  extends ApplicationExtension {

  final def run = {
    println("We are here: " + this.getClass)
    println("ich gehe jetzt schlafen für 5 sekunden!")
    Thread.sleep(5000)
    println("gähn. ich bin wieder aufgewacht.")
    Application.instance.teardown
    println("teardown")
  }

}
