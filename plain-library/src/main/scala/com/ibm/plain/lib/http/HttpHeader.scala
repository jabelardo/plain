package com.ibm.plain

package lib

package http

import java.text.SimpleDateFormat

import scala.Array.canBuildFrom

/**
 * A simple HttpHeader case class hierarchy.
 */
sealed abstract class HttpHeader {

  val name: String

  val value: String

  def render = name + ": " + value

}

/**
 * Predefined request headers, they can contain header specific logic and behavior.
 */
abstract sealed class PredefinedHttpHeader extends HttpHeader {

  val name = getClass.getSimpleName

}

sealed trait GeneralHttpHeader extends PredefinedHttpHeader

sealed trait RequestHttpHeader extends PredefinedHttpHeader

sealed trait ResponseHttpHeader extends PredefinedHttpHeader

sealed trait EntityHttpHeader extends PredefinedHttpHeader

/**
 * Helpers to parse the values of header fields.
 */

/**
 * HttpHeader.value contains a list of Tokens.
 */
trait TokenList {

  self: HttpHeader ⇒

  lazy val tokens = value.split(",").map(_.trim)

}

/**
 * HttpHeader.value contains an Int.
 */
trait IntValue {

  self: HttpHeader ⇒

  lazy val intValue = value.trim.toInt

}

/**
 * HttpHeader.value contains a java.util.Date.
 */
trait DateValue {

  self: HttpHeader ⇒

  lazy val dateValue = DateValue.format.parse(value.trim)

}

object DateValue {

  private val format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

}

/**
 * General header fields.
 */
case class `Cache-Control`(value: String) extends GeneralHttpHeader

case class `Connection`(value: String)

  extends GeneralHttpHeader

  with TokenList {

  def isKeepAlive = tokens.exists("keep-alive".equalsIgnoreCase)

  def isClose = tokens.exists("close".equalsIgnoreCase)

}

case class `Date`(value: String) extends GeneralHttpHeader with DateValue

case class `Pragma`(value: String) extends GeneralHttpHeader

case class `Trailer`(value: String) extends GeneralHttpHeader

case class `Transfer-Encoding`(value: String) extends GeneralHttpHeader

case class `Upgrade`(value: String) extends GeneralHttpHeader

case class `Via`(value: String) extends GeneralHttpHeader

case class `Warning`(value: String) extends GeneralHttpHeader

case class `X-Forwarded-For`(value: String) extends GeneralHttpHeader

/**
 * Request header fields.
 */
case class `Accept`(value: String) extends RequestHttpHeader

case class `Accept-Charset`(value: String) extends RequestHttpHeader

case class `Accept-Encoding`(value: String) extends RequestHttpHeader

case class `Accept-Language`(value: String) extends RequestHttpHeader

case class `Authorization`(value: String) extends RequestHttpHeader

case class `Expect`(value: String) extends RequestHttpHeader

case class `From`(value: String) extends RequestHttpHeader

case class `Host`(value: String) extends RequestHttpHeader

case class `If-Match`(value: String) extends RequestHttpHeader

case class `If-Modified-Since`(value: String) extends RequestHttpHeader

case class `If-None-Match`(value: String) extends RequestHttpHeader

case class `If-Range`(value: String) extends RequestHttpHeader

case class `If-Unmodified-Since`(value: String) extends RequestHttpHeader

case class `Max-Forwards`(value: String) extends RequestHttpHeader

case class `Proxy-Authorization`(value: String) extends RequestHttpHeader

case class `Range`(value: String) extends RequestHttpHeader

case class `Referer`(value: String) extends RequestHttpHeader

case class `TE`(value: String) extends RequestHttpHeader

case class `User-Agent`(value: String) extends RequestHttpHeader

/**
 * Entity header fields.
 */
case class `Allow`(value: String) extends EntityHttpHeader

case class `Content-Disposition`(value: String) extends EntityHttpHeader

case class `Content-Encoding`(value: String) extends EntityHttpHeader

case class `Content-Language`(value: String) extends EntityHttpHeader

case class `Content-Length`(value: String) extends EntityHttpHeader with IntValue

case class `Content-Location`(value: String) extends EntityHttpHeader

case class `Content-MD5`(value: String) extends EntityHttpHeader

case class `Content-Range`(value: String) extends EntityHttpHeader

case class `Content-Type`(value: String) extends EntityHttpHeader

case class `Expires`(value: String) extends EntityHttpHeader

case class `Last-Modified`(value: String) extends EntityHttpHeader with DateValue

/**
 * Response header fields.
 */
case class `Accept-Ranges`(value: String) extends ResponseHttpHeader

case class `Age`(value: String) extends ResponseHttpHeader

case class `ETag`(value: String) extends ResponseHttpHeader

case class `Location`(value: String) extends ResponseHttpHeader

case class `Proxy-Authenticate`(value: String) extends ResponseHttpHeader

case class `Retry-After`(value: String) extends ResponseHttpHeader

case class `Server`(value: String) extends ResponseHttpHeader

case class `Vary`(value: String) extends ResponseHttpHeader

case class `WWW-Authenticate`(value: String) extends ResponseHttpHeader

/**
 * Non-predefined header fields.
 */
case class `User-Defined`(name: String, value: String) extends HttpHeader

/**
 * The HttpHeader object.
 */
object HttpHeader {

  /**
   * Here we go... What about HttpHeader.getAllSubClasses?
   */
  def apply(name: String, value: String): HttpHeader = name.toLowerCase match {
    case "accept" ⇒ `Accept`(value)
    case "accept-charset" ⇒ `Accept-Charset`(value)
    case "accept-encoding" ⇒ `Accept-Encoding`(value)
    case "accept-language" ⇒ `Accept-Language`(value)
    case "accept-ranges" ⇒ `Accept-Ranges`(value)
    case "age" ⇒ `Age`(value)
    case "allow" ⇒ `Allow`(value)
    case "authorization" ⇒ `Authorization`(value)
    case "cache-control" ⇒ `Cache-Control`(value)
    case "connection" ⇒ `Connection`(value)
    case "content-disposition" ⇒ `Content-Disposition`(value)
    case "content-encoding" ⇒ `Content-Encoding`(value)
    case "content-language" ⇒ `Content-Language`(value)
    case "content-length" ⇒ `Content-Length`(value)
    case "content-location" ⇒ `Content-Location`(value)
    case "content-md5" ⇒ `Content-MD5`(value)
    case "content-range" ⇒ `Content-Range`(value)
    case "content-type" ⇒ `Content-Type`(value)
    case "date" ⇒ `Date`(value)
    case "etag" ⇒ `ETag`(value)
    case "expect" ⇒ `Expect`(value)
    case "expires" ⇒ `Expires`(value)
    case "from" ⇒ `From`(value)
    case "host" ⇒ `Host`(value)
    case "if-match" ⇒ `If-Match`(value)
    case "if-modified-since" ⇒ `If-Modified-Since`(value)
    case "if-none-match" ⇒ `If-None-Match`(value)
    case "if-range" ⇒ `If-Range`(value)
    case "if-unmodified-since" ⇒ `If-Unmodified-Since`(value)
    case "last-modified" ⇒ `Last-Modified`(value)
    case "location" ⇒ `Location`(value)
    case "max-forwards" ⇒ `Max-Forwards`(value)
    case "pragma" ⇒ `Pragma`(value)
    case "proxy-authenticate" ⇒ `Proxy-Authenticate`(value)
    case "proxy-authorization" ⇒ `Proxy-Authorization`(value)
    case "range" ⇒ `Range`(value)
    case "referer" ⇒ `Referer`(value)
    case "retry-after" ⇒ `Retry-After`(value)
    case "server" ⇒ `Server`(value)
    case "te" ⇒ `TE`(value)
    case "trailer" ⇒ `Trailer`(value)
    case "transfer-encoding" ⇒ `Transfer-Encoding`(value)
    case "upgrade" ⇒ `Upgrade`(value)
    case "user-agent" ⇒ `User-Agent`(value)
    case "vary" ⇒ `Vary`(value)
    case "via" ⇒ `Via`(value)
    case "warning" ⇒ `Warning`(value)
    case "www-authenticate" ⇒ `WWW-Authenticate`(value)
    case "x-forwarded-for" ⇒ `X-Forwarded-For`(value)
    case _ ⇒ `User-Defined`(name, value)
  }

}

