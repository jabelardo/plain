package com.ibm

package plain

package http

import java.nio.charset.Charset

import aio.{ ReadChannel, WriteChannel }

/**
 * Base class for the Entity of an Http request and/or response.
 */
sealed abstract class Entity {

  val contenttype: ContentType

  val length: Long

}

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  final case class ArrayEntity(array: Array[Byte], contenttype: ContentType) extends Entity { val length = array.length.toLong }

  final case class ContentEntity(contenttype: ContentType, length: Long) extends Entity

  final case class ReadChannelEntity(channel: ReadChannel, contenttype: ContentType, length: Long) extends Entity

  final case class `User-defined`(encoding: String, contenttype: ContentType) extends TransferEncodedEntity

  sealed abstract class TransferEncodedEntity extends Entity { val length = -1L }

  case class `identity`(contenttype: ContentType) extends TransferEncodedEntity

  case class `chunked`(contenttype: ContentType) extends TransferEncodedEntity

  case class `gzip`(contenttype: ContentType) extends TransferEncodedEntity

  case class `compress`(contenttype: ContentType) extends TransferEncodedEntity

  case class `deflate`(contenttype: ContentType) extends TransferEncodedEntity

  object TransferEncodedEntity {

    def apply(value: String, contenttype: ContentType): TransferEncodedEntity = value.toLowerCase match {
      case "identity" ⇒ `identity`(contenttype)
      case "chunked" ⇒ `chunked`(contenttype)
      case "gzip" ⇒ `gzip`(contenttype)
      case "compress" ⇒ `compress`(contenttype)
      case "deflate" ⇒ `deflate`(contenttype)
      case other ⇒ `User-defined`(other, contenttype)
    }

  }

}
