package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }
import java.util.zip.{ CRC32, DataFormatException, Deflater }

/**
 *
 */
final class GzipConduit private (

  protected[this] final val underlyingchannel: Channel,

  protected[this] final val compressionlevel: Int)

  extends GzipSourceConduit

  with GzipSinkConduit {

  protected[this] final val nowrap = true

}

/**
 *
 */
object GzipConduit {

  final def apply(underlyingchannel: Channel, compressionlevel: Int) = new GzipConduit(underlyingchannel, compressionlevel)

  final def apply(underlyingchannel: Channel) = new GzipConduit(underlyingchannel, deflaterCompressionLevel)

}

/**
 * Source conduit.
 */
sealed trait GzipSourceConduit

  extends DeflateSourceConduit {

  protected[this] override final def filterIn(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      readTrailer
      super.filterIn(processed, buffer)
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

  protected[this] override final def hasSufficient = if (header) 10 <= available else super.hasSufficient

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

  override protected[this] def filterOut(processed: Integer, buffer: ByteBuffer): Integer = {
    if (0 >= processed) {
      val read = deflater.getBytesRead
      super.filterOut(processed, buffer)
      writeTrailer(read)
    } else {
      if (header) writeHeader
      val e = buffer.position
      val len = super.filterOut(processed, buffer)
      checksum.update(buffer.array, e, len)
      len
    }
  }

  private[this] final def writeHeader = {
    innerbuffer.put(headerbytes)
    header = false
  }

  private[this] final def writeTrailer(bytesread: Long): Int = {
    def nextInt(l: Long) = {
      val i = l % 4294967296L
      innerbuffer.put((i & 0xff).toByte)
      innerbuffer.put(((i >> 8) & 0xff).toByte)
      innerbuffer.put(((i >> 16) & 0xff).toByte)
      innerbuffer.put(((i >> 24) & 0xff).toByte)
    }
    nextInt(checksum.getValue)
    nextInt(bytesread)
    0
  }

  private[this] final var header = true

  private[this] final val headerbytes = Array[Byte](0x1f, 0x8b.toByte, Deflater.DEFLATED, 0, 0, 0, 0, 0, 4, 0xff.toByte)

  private[this] final val checksum = new CRC32

}
