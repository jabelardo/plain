package com.ibm.plain

package lib

package http

import aio.Io

/**
 * Base class for the Entity of an Http request and/or response.
 */
abstract sealed class Entity

/**
 * The Entity object is a container for all concrete Entity types.
 */
object Entity {

  /**
   * The Entity that only knows how it will look like in the end.
   */
  case class ContentEntity(

    length: Int,

    typus: ContentType) extends Entity

  /**
   * The Entity represented by an Array[Byte] that was fully read together with the request header.
   */
  case class BytesEntity(bytes: Array[Byte]) extends Entity

  /**
   * The Entity represented by a String converted from a ByteBuffer using the specific Charset that was fully read together with the request header.
   */
  case class StringEntity(value: String) extends Entity

  /**
   * The Entity represented by an aio.Io instance, it is not fully read on creation and must be processed asynchronously.
   */
  case class IoEntity(io: Io) extends Entity

}

