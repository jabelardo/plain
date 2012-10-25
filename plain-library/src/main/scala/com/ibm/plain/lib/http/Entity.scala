package com.ibm.plain

package lib

package http

import java.nio.charset.Charset

import aio.{ AsynchronousReadChannel, AsynchronousWriteChannel }

/**
 * Base class for the Entity of an Http request and/or response.
 */
abstract sealed class Entity

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  case class ContentEntity(length: Int, typus: ContentType) extends Entity

  case class BytesEntity(bytes: Array[Byte]) extends Entity {

    override final def toString = "BytesEntity(length=" + bytes.length + ")"

  }

  case class StringEntity(bytes: Array[Byte], cset: Charset) extends Entity {

    final def value = new String(bytes, cset)

    override final def toString = "StringEntity(length=" + value.length + ", value=" + value.take(20) + "...)"

  }

  abstract sealed class ChannelEntity extends Entity

  case class RequestEntity(channel: AsynchronousReadChannel) extends ChannelEntity

  case class ResponseEntity(channel: AsynchronousWriteChannel) extends ChannelEntity

}

