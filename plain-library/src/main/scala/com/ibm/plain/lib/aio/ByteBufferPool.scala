package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

import scala.annotation.tailrec
import logging.HasLogger

final class ByteBufferPool private (buffersize: Int, initialpoolsize: Int)

  extends HasLogger {

  /**
   * This is an expensive O(n) operation, call it with care.
   */
  def size = pool.size

  @tailrec def getBuffer: ByteBuffer = {
    if (trylock) {
      try pool match {
        case head :: tail ⇒ pool = tail; head
        case Nil ⇒
          warning("ByteBufferPool exhausted : " + buffersize + ", " + initialpoolsize)
          ByteBuffer.allocateDirect(buffersize)
      } finally unlock

    } else {
      getBuffer
    }
  }

  @tailrec def releaseBuffer(buffer: ByteBuffer): Unit = if (buffer.isDirect) {
    if (trylock) {
      try {
        buffer.clear
        pool = buffer :: pool
      } finally unlock
    } else {
      releaseBuffer(buffer)
    }
  }

  @tailrec def clear: Unit = {
    if (trylock) {
      try pool = Nil
      finally unlock
    } else {
      clear
    }
  }

  @volatile private[this] var pool: List[ByteBuffer] = Nil

  @inline private[this] def trylock = locked.compareAndSet(false, true)

  @inline private[this] def unlock = locked.set(false)

  private[this] val locked = new AtomicBoolean(false)

  (0 until initialpoolsize).foreach(_ ⇒ releaseBuffer(ByteBuffer.allocateDirect(buffersize)))

  debug("ByteBufferPool preallocated : " + buffersize + ", " + initialpoolsize)

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
