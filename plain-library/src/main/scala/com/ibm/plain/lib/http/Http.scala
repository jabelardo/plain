package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import aio.Io

/**
 * Supported http versions. The current implementation only supports HTTP/1.1.
 */
sealed abstract class HttpVersion {

  final val version = getClass.getSimpleName

}

object HttpVersion {

  def apply(version: String): HttpVersion = version match {
    case "HTTP/1.0" ⇒ if (tread10VersionAs11) `HTTP/1.1` else `HTTP/1.0`
    case "HTTP/1.1" ⇒ `HTTP/1.1`
    case v ⇒
      if (treadAnyVersionAs11)
        `HTTP/1.1`
      else
        throw BadRequest("Unsupported http version " + v)
  }

}

case object `HTTP/1.0` extends HttpVersion
case object `HTTP/1.1` extends HttpVersion

/**
 * Supported http methods.
 */
sealed abstract class HttpMethod(val name: String)

object HttpMethod {

  def apply(name: String): HttpMethod = name match {
    case "GET" ⇒ GET
    case "HEAD" ⇒ HEAD
    case "PUT" ⇒ PUT
    case "POST" ⇒ POST
    case "DELETE" ⇒ DELETE
    case "OPTIONS" ⇒ OPTIONS
    case "CONNECT" ⇒ CONNECT
    case "TRACE" ⇒ TRACE
    case n ⇒ throw BadRequest("Invalid method " + n)
  }

}

case object GET extends HttpMethod("GET")
case object HEAD extends HttpMethod("HEAD")
case object PUT extends HttpMethod("PUT")
case object POST extends HttpMethod("POST")
case object DELETE extends HttpMethod("DELETE")
case object OPTIONS extends HttpMethod("OPTIONS")
case object CONNECT extends HttpMethod("CONNECT")
case object TRACE extends HttpMethod("TRACE")

/**
 * Base class for the body of an HttpRequest.
 */
abstract sealed class HttpRequestBody

/**
 * The 'non-existent' request body.
 */
case object NoneRequestBody extends HttpRequestBody

/**
 * The body represented by an Array[Byte] that was fully read together with the request header.
 */
case class BytesRequestBody(bytes: Array[Byte]) extends HttpRequestBody

/**
 * The body represented by a String converted from a ByteBuffer that was fully read together with the request header.
 */
case class StringRequestBody(value: String) extends HttpRequestBody

/**
 * The body represented by an Io instance, it is incomplete on creation and must be processed asynchronously.
 */
case class IoRequestBody(io: Io) extends HttpRequestBody

/**
 * Http error handling.
 */
sealed abstract class HttpException(message: String) extends Exception(message)

case class BadRequest(message: String) extends HttpException(message)

/**
 * The classic http request.
 */
case class HttpRequest(
  method: HttpMethod,
  path: List[String],
  query: Option[String],
  version: HttpVersion,
  headers: List[HttpHeader],
  body: HttpRequestBody)

