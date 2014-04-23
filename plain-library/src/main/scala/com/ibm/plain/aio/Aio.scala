package com.ibm

package plain

package aio

import bootstrap.{ BaseComponent, IsSingleton, Singleton }

/**
 *
 */
final class Aio private

  extends BaseComponent[Aio]("plain-aio")

  with IsSingleton {

  final val defaultBufferPool = ByteBufferPool(defaultBufferSize, defaultBufferPoolSize)

  final val tinyBufferPool = ByteBufferPool(tinyBufferSize, tinyBufferPoolSize)

  final val largeBufferPool = ByteBufferPool(largeBufferSize, largeBufferPoolSize)

  final val hugeBufferPool = ByteBufferPool(hugeBufferSize, hugeBufferPoolSize)

}

/**
 * The Aio object.
 */
object Aio

  extends Singleton[Aio](new Aio)

