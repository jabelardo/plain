package com.ibm.plain

package lib

package aio

import scala.concurrent.util.Duration

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Aio

  extends BaseComponent[Aio]("plain-aio") {

  override def start = {
    if (isEnabled) {
      defaultBufferPool
      largeBufferPool
    }
    this
  }

  override def stop = {
    if (isStarted) {
      defaultBufferPool.clear
      largeBufferPool.clear
    }
    this
  }

  final lazy val defaultBufferPool = ByteBufferPool(defaultBufferSize, defaultBufferPoolSize)

  final lazy val largeBufferPool = ByteBufferPool(largeBufferSize, largeBufferPoolSize)

}

/**
 * The Aio object.
 */
object Aio extends Aio

