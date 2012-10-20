package com.ibm.plain

package lib

package http

import java.text.SimpleDateFormat

import scala.Array.canBuildFrom

/**
 * A simple Header case class hierarchy.
 */
sealed abstract class Header {

  def name: String

  val value: String

  override def equals(that: Any) = reflect.strippedName(getClass) == reflect.strippedName(that.getClass)

  override def hashCode = name.hashCode

  final def render = name + ": " + value

}

/**
 * The Header object.
 */
object Header {

  /**
   * Predefined request headers, they can contain header specific logic and behavior.
   */
  abstract sealed class PredefinedHeader extends Header {

    final def name = reflect.simpleName(getClass)

  }

  sealed trait General extends PredefinedHeader

  sealed trait Request extends PredefinedHeader

  sealed trait Response extends PredefinedHeader

  sealed trait Entity extends PredefinedHeader

  /**
   * Helpers to parse the values of header fields.
   */
  trait Value { val value: String }

  object Value {

    /**
     * Header.value contains a list of Tokens.
     */
    trait TokenList extends Value { final def tokens = value.split(",").map(_.trim) }

    /**
     * Header.value contains an Int.
     */
    trait IntValue extends Value { final def intValue = value.trim.toInt }

    /**
     * Header.value contains a java.util.Date.
     */
    trait DateValue extends Value { final def dateValue = dateformat.parse(value.trim) }

    /**
     * The DateValue object provides the SimpleDateFormat used in http header fields.
     */
    private final val dateformat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z")

  }

  import Value._

  import General._
  import Request._
  import Response._
  import Entity._

  /**
   * Map header.name.toLowerCase with its corresponding case class.
   */
  def apply(name: String, value: String): Header = name.toLowerCase match {
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
    case userdefined ⇒ `User-Defined`(userdefined, value)
  }

  /**
   * General header fields.
   */
  object General {

    case class `Cache-Control`(value: String) extends General

    case class `Connection`(value: String)

      extends General

      with TokenList {

      def isKeepAlive = tokens.exists("keep-alive".equalsIgnoreCase)

      def isClose = tokens.exists("close".equalsIgnoreCase)

    }
  }
  case class `Date`(value: String) extends General with DateValue

  case class `Pragma`(value: String) extends General

  case class `Trailer`(value: String) extends General

  case class `Transfer-Encoding`(value: String) extends General

  case class `Upgrade`(value: String) extends General

  case class `Via`(value: String) extends General

  case class `Warning`(value: String) extends General

  case class `X-Forwarded-For`(value: String) extends General

  /**
   * Request header fields.
   */
  object Request {

    case class `Accept`(value: String) extends Request

    case class `Accept-Charset`(value: String) extends Request

    case class `Accept-Encoding`(value: String) extends Request

    case class `Accept-Language`(value: String) extends Request

    case class `Authorization`(value: String) extends Request

    case class `Expect`(value: String) extends Request

    case class `From`(value: String) extends Request

    case class `Host`(value: String) extends Request

    case class `If-Match`(value: String) extends Request

    case class `If-Modified-Since`(value: String) extends Request

    case class `If-None-Match`(value: String) extends Request

    case class `If-Range`(value: String) extends Request

    case class `If-Unmodified-Since`(value: String) extends Request

    case class `Max-Forwards`(value: String) extends Request

    case class `Proxy-Authorization`(value: String) extends Request

    case class `Range`(value: String) extends Request

    case class `Referer`(value: String) extends Request

    case class `TE`(value: String) extends Request

    case class `User-Agent`(value: String) extends Request

  }

  /**
   * Entity header fields.
   */
  object Entity {

    case class `Allow`(value: String) extends Entity

    case class `Content-Disposition`(value: String) extends Entity

    case class `Content-Encoding`(value: String) extends Entity

    case class `Content-Language`(value: String) extends Entity

    case class `Content-Length`(value: String) extends Entity with IntValue

    case class `Content-Location`(value: String) extends Entity

    case class `Content-MD5`(value: String) extends Entity

    case class `Content-Range`(value: String) extends Entity

    case class `Content-Type`(value: String) extends Entity

    case class `Expires`(value: String) extends Entity

    case class `Last-Modified`(value: String) extends Entity with DateValue

  }
  /**
   * Response header fields.
   */
  object Response {

    case class `Accept-Ranges`(value: String) extends Response

    case class `Age`(value: String) extends Response

    case class `ETag`(value: String) extends Response

    case class `Location`(value: String) extends Response

    case class `Proxy-Authenticate`(value: String) extends Response

    case class `Retry-After`(value: String) extends Response

    case class `Server`(value: String) extends Response

    case class `Vary`(value: String) extends Response

    case class `WWW-Authenticate`(value: String) extends Response

  }
  /**
   * Non-predefined header fields.
   */
  class `User-Defined` private (val name: String, val value: String) extends Header

  object `User-Defined` {

    def apply(name: String, value: String) = new `User-Defined`(name.toLowerCase, value)

  }

}
