package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.zip.{ CRC32, Inflater, DataFormatException }

/**
 *
 */
final class GzipConduit private (

  protected[this] val underlyingchannel: Channel)

  extends GzipSourceConduit

  with GzipSinkConduit {

  protected[this] final val nowrap = true

}

/**
 *
 */
object GzipConduit {

  final def apply(underlyingchannel: Channel) = new GzipConduit(underlyingchannel)

}

/**
 * Source conduit.
 */
sealed trait GzipSourceConduit

  extends DeflateSourceConduit {

  protected[this] override def filterIn(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      readTrailer
      processed
    } else {
      if (header) readHeader
      val e = buffer.position
      val len = super.filterIn(processed, buffer)
      if (!ignoreChecksumForGzipDecoding) {
        checksum.update(buffer.array, e, len)
      }
      len
    }
  }

  protected[this] override def hasSufficient = if (header) 10 <= available else super.hasSufficient

  private[this] final def readHeader = {
    def nextByte = 0xff & innerbuffer.get
    def nextShort = nextByte | (nextByte << 8)
    def skipString = while (0 != nextByte) {}
    val e = innerbuffer.position
    def crc16 = { checksum.update(innerbuffer.array, e, innerbuffer.position); checksum.getValue.toShort }
    require(0x1f == nextByte, invalidFormat())
    require(0x8b == nextByte, invalidFormat())
    require(0x08 == nextByte, invalidFormat())
    val flags = nextByte
    skip(6)
    def isSet(flag: Byte) = { 0 < ((1 << flag) & flags) }
    if (isSet(2)) skip(2)
    if (isSet(3)) skipString
    if (isSet(4)) skipString
    if (isSet(1)) require(crc16 == nextShort)
    checksum.reset
    header = false
  }

  private[this] final def readTrailer = {
    def nextByte = 0xff & innerbuffer.get
    def nextShort = nextByte | (nextByte << 8)
    def nextInt = nextShort | (nextShort << 16)
    if (ignoreChecksumForGzipDecoding) {
      skip(8)
    } else {
      if (Int.MaxValue > checksum.getValue) {
        require(nextInt == checksum.getValue.toInt, invalidFormat("crc32 error in trailer"))
      } else {
        skip(4)
      }
      if (Int.MaxValue > inflater.getBytesWritten) {
        require(nextInt == inflater.getBytesWritten.toInt, invalidFormat("length mismatch error in trailer"))
      } else {
        skip(4)
      }
    }
    /**
     * We do not support gzip "extensions" here. I have never actually seen them in practice.S
     */
  }

  private[this] final def invalidFormat(message: String = null) = throw new DataFormatException(message)

  private[this] final var header = true

  private[this] final val checksum = new CRC32

}

/**
 * Sink conduit.
 */
sealed trait GzipSinkConduit

  extends DeflateSinkConduit {

}

