package com.ibm

package plain

package aio

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

import scala.annotation.tailrec

import logging.createLogger
import concurrent.OnlyOnce

final class ByteBufferPool private (buffersize: Int, initialpoolsize: Int)

  extends OnlyOnce {

  /**
   * This is an expensive O(n) operation.
   */
  final def size = pool.size

  @tailrec final def get: ByteBuffer = if (trylock) {
    val buffer = try pool match {
      case head :: tail ⇒
        if (0 < watermark) watermark -= 1
        pool = tail
        head
      case _ ⇒
        null
    } finally unlock
    if (null != buffer) {
      buffer.clear
      buffer
    } else {
      onlyonce { createLogger(this).warn("ByteBufferPool exhausted, need to create more : buffer size " + buffersize + ", initial pool size " + initialpoolsize) }
      ByteBuffer.allocate(buffersize)
    }
  } else {
    Thread.`yield`
    get
  }

  /**
   * Todo: Remove check to prevent double releases, it's expensive.
   */
  @tailrec final def release(buffer: ByteBuffer): Unit = if (trylock) {
    try {
      // if (!pool.exists(_ eq buffer)) {
      buffer.clear
      pool = buffer :: pool
      // } else {
      //  createLogger(this).warn("Trying to release twice (prevented) " + pool.size + ", buffer size " + buffersize + ", initial pool size " + initialpoolsize)
      // }
    } finally unlock
  } else {
    Thread.`yield`
    release(buffer)
  }

  @inline private[this] final def trylock = locked.compareAndSet(false, true)

  @inline private[this] final def unlock = locked.set(false)

  @volatile private[this] final var pool: List[ByteBuffer] = (0 until initialpoolsize).map(_ ⇒ ByteBuffer.allocate(buffersize)).toList

  @volatile private[this] final var watermark = initialpoolsize

  private[this] final val locked = new AtomicBoolean(false)

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
