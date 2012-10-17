package com.ibm.plain

package lib

package aio

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Aio

  extends BaseComponent[Aio]("plain-aio") {

  override def start = {
    if (isEnabled) {
      defaultBufferPool
      tinyBufferPool
      largeBufferPool
    }
    this
  }

  final lazy val defaultBufferPool = ByteBufferPool(defaultBufferSize, defaultBufferPoolSize)

  final lazy val tinyBufferPool = ByteBufferPool(tinyBufferSize, tinyBufferPoolSize)

  final lazy val largeBufferPool = ByteBufferPool(largeBufferSize, largeBufferPoolSize)

}

/**
 * The Aio object.
 */
object Aio extends Aio

