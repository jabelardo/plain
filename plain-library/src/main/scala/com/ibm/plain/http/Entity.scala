package com.ibm

package plain

package http

import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.CoderResult.OVERFLOW
import java.nio.charset.Charset
import java.nio.channels.AsynchronousByteChannel

import aio.{ bestFitByteBuffer, releaseByteBuffer }
import text.`UTF-8`
import Status._

/**
 * Base class for the Entity of an Http request and/or response.
 */
sealed abstract class Entity {

  val contenttype: ContentType

  val length: Long

  val encodable: Boolean

}

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  final case class ArrayEntity(array: Array[Byte], offset: Int, length: Long, contenttype: ContentType) extends Entity {

    final val encodable = aio.tooTinyToCareSize < length

  }

  object ArrayEntity {

    final def apply(array: Array[Byte], contenttype: ContentType): ArrayEntity = ArrayEntity(array, 0, array.length, contenttype)

  }

  final case class ByteBufferEntity(buffer: ByteBuffer, contenttype: ContentType) extends Entity {

    val length: Long = buffer.remaining

    val encodable = aio.tooTinyToCareSize < length

  }

  object ByteBufferEntity {

    final def apply(s: String, contenttype: ContentType): ByteBufferEntity = {
      var factor = 1.1
      var buffer: ByteBuffer = null
      while (6.0 >= factor) {
        if (null != buffer) releaseByteBuffer(buffer)
        buffer = bestFitByteBuffer((s.length * factor).toInt)
        `UTF-8`.newEncoder.encode(CharBuffer.wrap(s), buffer, true) match {
          case OVERFLOW ⇒ factor *= 2.0
          case _ ⇒ buffer.flip; return new ByteBufferEntity(buffer, contenttype)
        }
      }
      throw ServerError.`500`
    }

  }

  final case class ContentEntity(contenttype: ContentType, length: Long) extends Entity { val encodable = false }

  final case class AsynchronousByteChannelEntity(channel: AsynchronousByteChannel, contenttype: ContentType, length: Long, encodable: Boolean) extends Entity

  final case class `User-defined`(encoding: String, contenttype: ContentType) extends TransferEncodedEntity

  sealed abstract class TransferEncodedEntity extends Entity { val length = -1L; val encodable = true }

  final case class `identity`(contenttype: ContentType) extends TransferEncodedEntity

  final case class `chunked`(contenttype: ContentType) extends TransferEncodedEntity

  final case class `gzip`(contenttype: ContentType) extends TransferEncodedEntity

  final case class `compress`(contenttype: ContentType) extends TransferEncodedEntity

  final case class `deflate`(contenttype: ContentType) extends TransferEncodedEntity

  object TransferEncodedEntity {

    final def apply(value: String, contenttype: ContentType): TransferEncodedEntity = value.toLowerCase match {
      case "identity" ⇒ `identity`(contenttype)
      case "chunked" ⇒ `chunked`(contenttype)
      case "gzip" ⇒ `gzip`(contenttype)
      case "compress" ⇒ `compress`(contenttype)
      case "deflate" ⇒ `deflate`(contenttype)
      case other ⇒ `User-defined`(other, contenttype)
    }

  }

}
