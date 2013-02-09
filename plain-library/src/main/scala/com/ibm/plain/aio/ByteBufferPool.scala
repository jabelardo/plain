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
  def size = pool.size

  @tailrec def getBuffer: ByteBuffer = if (trylock) {
    try pool match {
      case head :: tail ⇒
        pool = tail
        head
      case Nil ⇒
        onlyonce { warning("ByteBufferPool exhausted : buffer size " + buffersize + ", initial pool size" + initialpoolsize) }
        ByteBuffer.allocateDirect(buffersize)
    } finally unlock
  } else {
    Thread.sleep(0, 50) // removing this will degrade performance dramatically
    getBuffer
  }

  @tailrec def releaseBuffer(buffer: ByteBuffer): Unit = if (trylock) {
    try {
      // if (log.isDebugEnabled) { debug(pool.size.toString); require(!pool.exists(_ eq buffer), "buffer released twice " + pool.size) }
      buffer.clear
      pool = buffer :: pool
    } finally unlock
  } else {
    Thread.sleep(0, 50) 
    releaseBuffer(buffer)
  }

  @inline private[this] final def trylock = locked.compareAndSet(false, true)

  @inline private[this] final def unlock = locked.set(false)

  @volatile private[this] var pool: List[ByteBuffer] =
    (0 until initialpoolsize).toList.map(_ ⇒ ByteBuffer.allocateDirect(buffersize))

  private[this] final val locked = new AtomicBoolean(false)

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
