package com.ibm

package plain

package aio

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Aio

  extends BaseComponent[Aio]("plain-aio") {

  final val defaultBufferPool = ByteBufferPool(defaultBufferSize, defaultBufferPoolSize)

  final val tinyBufferPool = ByteBufferPool(tinyBufferSize, tinyBufferPoolSize)

  final val largeBufferPool = ByteBufferPool(largeBufferSize, largeBufferPoolSize)

  final val hugeBufferPool = ByteBufferPool(hugeBufferSize, hugeBufferPoolSize)

}

/**
 * The Aio object.
 */
object Aio extends Aio

