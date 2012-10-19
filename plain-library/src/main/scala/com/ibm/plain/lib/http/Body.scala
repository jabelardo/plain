package com.ibm.plain

package lib

package http

import aio.Io

/**
 * Base class for the body of an HttpRequest.
 */
abstract sealed class RequestBody

object RequestBody {

  /**
   * The body represented by an Array[Byte] that was fully read together with the request header.
   */
  case class BytesRequestBody(bytes: Array[Byte]) extends RequestBody

  /**
   * The body represented by a String converted from a ByteBuffer using the specific Charset that was fully read together with the request header.
   */
  case class StringRequestBody(value: String) extends RequestBody

  /**
   * The body represented by an aio.Io instance, it is not fully read on creation and must be processed asynchronously.
   */
  case class IoRequestBody(io: Io) extends RequestBody

}

