package com.ibm

package plain

package aio

import java.nio.{ ByteBuffer, ByteOrder }
import java.util.concurrent.atomic.AtomicBoolean

import scala.annotation.tailrec

import logging.HasLogger
import concurrent.OnlyOnce

final class ByteBufferPool private (buffersize: Int, initialpoolsize: Int)

  extends HasLogger

  with OnlyOnce {

  /**
   * This is an expensive O(n) operation.
   */
  final def size = pool.size

  @tailrec final def get: ByteBuffer = if (trylock) {
    try pool match {
      case head :: tail ⇒
        pool = tail
        head
      case Nil ⇒
        onlyonce { warning("ByteBufferPool exhausted : buffer size " + buffersize + ", initial pool size " + initialpoolsize) }
        ByteBuffer.allocateDirect(buffersize)
    } finally unlock
  } else {
    Thread.`yield`
    get
  }

  @tailrec final def release(buffer: ByteBuffer): Unit = if (trylock) {
    try {
      // require(!pool.exists(_ eq buffer), "buffer released twice " + pool.size)
      buffer.clear
      pool = buffer :: pool
      // debug("current " + pool.size + ", buffer size " + buffersize + ", initial pool size " + initialpoolsize)
    } finally unlock
  } else {
    Thread.`yield`
    release(buffer)
  }

  @inline private[this] final def trylock = locked.compareAndSet(false, true)

  @inline private[this] final def unlock = locked.set(false)

  @volatile private[this] final var pool: List[ByteBuffer] = (0 until initialpoolsize).map(_ ⇒ ByteBuffer.allocateDirect(buffersize)).toList

  private[this] final val locked = new AtomicBoolean(false)

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
