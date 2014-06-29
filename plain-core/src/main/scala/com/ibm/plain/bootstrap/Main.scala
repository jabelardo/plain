package com.ibm

package plain

package bootstrap

/**
 *
 */
object Main

    extends App {

  /**
   * Commons logging interferes with plain-logging. Please keep silent.
   */
  System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

  run

}
