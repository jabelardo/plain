package com.ibm

package plain

package http

import java.nio.charset.Charset

import aio.{ ReadChannel, WriteChannel }

/**
 * Base class for the Entity of an Http request and/or response.
 */
sealed abstract class Entity

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  final case class ArrayEntity(array: Array[Byte], contenttype: ContentType) extends Entity { println(this); println(array.length) }

  final case class StringEntity(s: String, contenttype: ContentType) extends Entity { println(this) }

  final case class ContentEntity(length: Long, contenttype: ContentType) extends Entity

  final case class `User-defined`(encoding: String) extends TransferEncodedEntity

  sealed abstract class TransferEncodedEntity extends Entity

  case object `identity` extends TransferEncodedEntity

  case object `chunked` extends TransferEncodedEntity

  case object `gzip` extends TransferEncodedEntity

  case object `compress` extends TransferEncodedEntity

  case object `deflate` extends TransferEncodedEntity

  object TransferEncodedEntity {

    def apply(value: String) = value.toLowerCase match {
      case "identity" ⇒ `identity`
      case "chunked" ⇒ `chunked`
      case "gzip" ⇒ `gzip`
      case "compress" ⇒ `compress`
      case "deflate" ⇒ `deflate`
      case other ⇒ `User-defined`(other)
    }

  }

}
