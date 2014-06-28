package com.ibm

package plain

package bootstrap

/**
 * Marker trait. The companion object of a class inheriting from this trait should inherit from Singleton[ThatClass] and call ThatObject.setInstance.
 */
trait IsSingleton

/**
 * You need to inherit the companion object of a class extending IsSingleton from this trait.
 */
abstract class Singleton[A <: IsSingleton](

  /**
   * Maybe null on object creation, in this case instance(value) must be called later.
   */
  initialinstance: A) {

  final def this() = this(null.asInstanceOf[A])

  final def instance: A = instanceholder

  final def instance(instance: A) = instanceholder = instance

  final def resetInstance = instanceholder = null.asInstanceOf[A]

  @volatile private[this] final var instanceholder: A = initialinstance

}

