package com.ibm.plain

package lib

package aio

import java.nio.ByteBuffer
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
    getBuffer
  }

  @tailrec def releaseBuffer(buffer: ByteBuffer): Unit = if (trylock) {
    try {
      buffer.clear
      pool = buffer :: pool
    } finally unlock
  } else {
    releaseBuffer(buffer)
  }

  @volatile private[this] var pool: List[ByteBuffer] = (0 until initialpoolsize).toList.map(_ ⇒ ByteBuffer.allocateDirect(buffersize))

  @inline private[this] def trylock = locked.compareAndSet(false, true)

  @inline private[this] def unlock = locked.set(false)

  private[this] val locked = new AtomicBoolean(false)

  debug("ByteBufferPool preallocated : " + buffersize + ", " + initialpoolsize)

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
