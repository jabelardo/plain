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

  final case class ContentEntity(

    var length: Long,

    var contenttype: ContentType)

    extends Entity {

    final def ++(length: Long) = { this.length = length; this }

    final def ++(contenttype: ContentType) = { this.contenttype = contenttype; this }

  }

  object ContentEntity {

    @inline def apply(length: Long) = new ContentEntity(length, null)

  }

  sealed abstract class TransferEncodedEntity extends Entity

  case object `identity` extends TransferEncodedEntity

  case object `chunked` extends TransferEncodedEntity

  case object `gzip` extends TransferEncodedEntity

  case object `compress` extends TransferEncodedEntity

  case object `deflate` extends TransferEncodedEntity

  final case class `user-defined`(encoding: String) extends TransferEncodedEntity

  object TransferEncodedEntity {

    @inline def apply(value: String) = value.toLowerCase match {
      case "identity" ⇒ `identity`
      case "chunked" ⇒ `chunked`
      case "gzip" ⇒ `gzip`
      case "compress" ⇒ `compress`
      case "deflate" ⇒ `deflate`
      case other ⇒ `user-defined`(other)
    }

  }

}
