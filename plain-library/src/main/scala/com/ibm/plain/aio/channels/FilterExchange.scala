package com.ibm

package plain

package aio

package channels

import java.nio.ByteBuffer

/**
 *
 */
final class FilterExchange[A] private (

  protected[this] final val readbuffer: ByteBuffer,

  protected[this] final val writebuffer: ByteBuffer)

  extends ExchangeIo[A]

  with ExchangeIoImpl[A] 

