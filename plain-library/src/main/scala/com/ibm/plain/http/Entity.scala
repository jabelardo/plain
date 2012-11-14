package com.ibm

package plain

package http

import java.nio.charset.Charset

import aio.{ ReadChannel, WriteChannel }

/**
 * Base class for the Entity of an Http request and/or response.
 */
abstract sealed class Entity

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  class ContentEntity(val _1: Long, val _2: ContentType) extends Entity
  
  object ContentEntity {
    
    def apply(length: Long, typus: ContentType) = new ContentEntity(length, typus)
    
    def unapply(e: ContentEntity): Option[(Long, ContentType)] = Some(e._1, e._2)
    
  }
  
  case class StringEntity(s: String, typus: ContentType) extends ContentEntity(s.length, typus)

  case class BytesEntity(bytes: Array[Byte]) extends Entity {

    override final def toString = "BytesEntity(length=" + bytes.length + ")"

  }

  // remove
  case class StringEntity2(bytes: Array[Byte], cset: Charset) extends Entity {

    final def value = new String(bytes, cset)

    override final def toString = "StringEntity(length=" + value.length + ", value=" + value.take(20) + "...)"

  }

  abstract sealed class ChannelEntity extends Entity

  case class RequestEntity(channel: ReadChannel) extends ChannelEntity

  case class ResponseEntity(channel: WriteChannel) extends ChannelEntity

}

