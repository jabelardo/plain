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

  final case class ContentEntity(length: Long) extends Entity

  sealed abstract class TransferEncodedEntity extends Entity

  case object `identity` extends TransferEncodedEntity

  case object `chunked` extends TransferEncodedEntity

  case object `gzip` extends TransferEncodedEntity

  case object `compress` extends TransferEncodedEntity

  case object `deflate` extends TransferEncodedEntity

  final case class `User-defined`(encoding: String) extends TransferEncodedEntity

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
