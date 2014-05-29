package com.ibm

package plain

package bootstrap

/**
 * Implement any number of classes of type ApplicationExtension to be called after the Application bootstrap process has finished.
 * Note that their run method will be called in a non-deterministic order. run should be short and return Unit. 
 * Of course a run method can end with a call to Application.instance.teardown for short tests.
 */
trait ApplicationExtension {

  def run

}