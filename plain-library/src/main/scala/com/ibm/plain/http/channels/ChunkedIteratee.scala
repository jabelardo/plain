package com.ibm

package plain

package http

package channels

import java.nio.ByteBuffer

import aio.{ ExchangeIo, Iteratee, ExchangeIteratee }
import aio.Iteratees.{ drop, takeBytes, takeUntil, takeWhile }
import http.RequestConstants.hex
import text.ascii

/**
 *
 */
case class Chunk(buffer: ByteBuffer) { println(aio.format(buffer)) }

/**
 *
 */
object ChunkedIteratee {

  final def next[A]: Iteratee[ExchangeIo[A], Chunk] = {
    for {
      _ ← takeUntil(hex, ascii, false)
      len ← takeWhile(hex, ascii, false)
      _ ← drop(2)
      array ← takeBytes(Integer.parseInt(len, 16))
    } yield Chunk(ByteBuffer.wrap(array))
  }

}
