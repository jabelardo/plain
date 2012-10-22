package com.ibm.plain

package lib

package aio

import java.nio.{ ByteBuffer, ByteOrder }

import scala.collection.mutable.DoubleLinkedList

import concurrent.OnlyOnce
import logging.HasLogger

final class ByteBufferPool private (buffersize: Int, initialpoolsize: Int)

  extends HasLogger

  with OnlyOnce {

  def size = synchronized(pool.size)

  def getBuffer: ByteBuffer = synchronized {
    pool match {
      case head :: tail ⇒
        pool = tail
        head
      case _ ⇒
        onlyonce { warning("ByteBufferPool exhausted : buffer size " + buffersize + ", initial pool size" + initialpoolsize) }
        ByteBuffer.allocateDirect(buffersize).order(ByteOrder.nativeOrder)
    }
  }

  def releaseBuffer(buffer: ByteBuffer) = synchronized {
    buffer.clear
    pool = buffer :: pool
  }

  private[this] var pool: List[ByteBuffer] = (0 until initialpoolsize).toList.map(_ ⇒ ByteBuffer.allocateDirect(buffersize).order(ByteOrder.nativeOrder))

}

object ByteBufferPool {

  def apply(buffersize: Int, initialpoolsize: Int) = new ByteBufferPool(buffersize, initialpoolsize: Int)

}
