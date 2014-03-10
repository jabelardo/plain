package com.ibm

package plain

package bootstrap

/**
 * Instances of this class will be defined outside this library and will be loaded and bootstrapped at runtime.
 * They must provide an ordering if they depend on other components.
 */
abstract class ExternalComponent[C](name: String)

  extends BaseComponent[C](name) {

  /**
   * The higher the value of "order" the earlier components will be started. 0 or below means 'I don't care.'.
   */
  def order = -1

}