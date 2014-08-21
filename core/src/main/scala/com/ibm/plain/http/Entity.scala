package com.ibm

package plain

package http

import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.CoderResult.OVERFLOW
import java.nio.charset.Charset
import java.nio.channels.AsynchronousByteChannel

import aio.{ bestFitByteBuffer, releaseByteBuffer, Exchange, ExchangeHandler, Encoding }
import aio.conduit.Conduit
import text.`UTF-8`
import Status._
import ServerError.`501`

/**
 * Base class for the Entity of an Http request and/or response. An Entity can be read or written. (That doesn't make it easier.)
 */
sealed abstract class Entity {

  val contenttype: ContentType

  val length: Long

  val encodable: Boolean

  val contentencoding: Option[Encoding]

  val transferencoding: Option[Encoding]

}

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  final def unapply(entity: Entity): Option[(ContentType, Long, Boolean)] = Some((entity.contenttype, entity.length, entity.encodable))

  /**
   *
   */
  final case class ArrayEntity(

    array: Array[Byte],

    offset: Int,

    length: Long,

    contenttype: ContentType)

    extends Entity {

    final val encodable = true

    final val contentencoding = None

    final val transferencoding = None

  }

  /**
   *
   */
  object ArrayEntity {

    final def apply(array: Array[Byte], contenttype: ContentType): ArrayEntity = ArrayEntity(array, 0, array.length, contenttype)

  }

  /**
   *
   */
  final case class ByteBufferEntity(

    buffer: ByteBuffer,

    contenttype: ContentType)

    extends Entity {

    final val length: Long = buffer.remaining

    final val encodable = true

    final val contentencoding = None

    final val transferencoding = None

  }

  object ByteBufferEntity {

    final def apply(s: String, contenttype: ContentType): ByteBufferEntity = {
      new ByteBufferEntity(ByteBuffer.wrap(s.getBytes(contenttype.charset.toString)), contenttype)
    }

  }

  /**
   *
   */
  final case class ContentEntity(

    contenttype: ContentType,

    length: Long,

    encodable: Boolean,

    contentencoding: Option[Encoding],

    transferencoding: Option[Encoding])

    extends Entity

  /**
   *
   */
  object ContentEntity {

    final def apply(contenttype: ContentType, length: Long) = new ContentEntity(contenttype, length, false, None, None)

    final def apply(contenttype: ContentType, contentencoding: Option[Encoding]) = new ContentEntity(contenttype, -1, true, contentencoding, None)

  }

  /**
   *
   */
  final case class ConduitEntity(

    conduit: Conduit,

    contenttype: ContentType,

    length: Long,

    encodable: Boolean)

    extends Entity {

    final val contentencoding = None

    final val transferencoding = None

  }

  /**
   *
   */
  sealed abstract class TransferEncodedEntity(

    val transferencoding: Option[Encoding])

    extends Entity {

    val entity: Entity

    val length = -1L

    val encodable = true

    val contenttype = entity.contenttype

    val contentencoding = entity.contentencoding

  }

  /**
   *
   */
  final case class `identity`(

    entity: Entity)

    extends TransferEncodedEntity(Some(Encoding.`identity`))

  /**
   *
   */
  final case class `chunked`(

    entity: Entity)

    extends TransferEncodedEntity(Some(Encoding.`chunked`))

  /**
   *
   */
  final case class `deflate`(

    entity: Entity)

    extends TransferEncodedEntity(Some(Encoding.`chunked`)) {

    override final val contentencoding = Some(Encoding.`deflate`)

  }

  /**
   *
   */
  final case class `gzip`(

    entity: Entity)

    extends TransferEncodedEntity(Some(Encoding.`chunked`)) {

    override final val contentencoding = Some(Encoding.`gzip`)

  }

  /**
   *
   */
  object TransferEncodedEntity {

    @inline final def apply(value: String, entity: Entity): TransferEncodedEntity = value match {
      case "identity" ⇒ `identity`(entity)
      case "chunked" ⇒ `chunked`(entity)
      case _ ⇒ throw `501`
    }

    @inline final def unapply(entity: TransferEncodedEntity): Option[(Entity, Option[Encoding])] =
      Some((entity.entity, entity.transferencoding))

  }

}
