package com.ibm

package plain

package bootstrap

/**
 * Instances of this class will be defined outside this library and will be loaded and bootstrapped at runtime.
 * They must provide an ordering if they depend on other components.
 */
abstract class ExternalComponent[C](

  enabled: Boolean,

  name: String,

  dependants: Class[_ <: Component[_]]*)

  extends BaseComponent[C](enabled, name, dependants: _*)

  with IsSingleton {

  final def this(enabled: Boolean, name: String) = this(enabled, name, Seq[Class[Component[_]]](): _*)

  /**
   * The lower the value of "order" the earlier components will be started.
   */
  def order = Int.MaxValue

}