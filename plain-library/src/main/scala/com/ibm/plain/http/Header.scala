package com.ibm

package plain

package http

import Message.Headers

/**
 * A maybe not so simple Header case class hierarchy.
 */
sealed abstract class Header[A]

  extends (Headers ⇒ Option[A]) {

  def name: String

  def value(s: String): A

  final def apply(headers: Headers): Option[A] = headers.get(name) match {
    case Some(s) ⇒ Some(value(s))
    case _ ⇒ None
  }

}

/**
 * The Header object.
 */
object Header {

  /**
   * Predefined request headers, they can contain header specific logic and behavior.
   */
  sealed abstract class PredefinedHeader[A]

    extends Header[A] {

    final val name = reflect.scalifiedName(getClass).toLowerCase

  }

  sealed abstract class General[A] extends PredefinedHeader[A]

  sealed abstract class Request[A] extends PredefinedHeader[A]

  sealed abstract class Response[A] extends PredefinedHeader[A]

  sealed abstract class Entity[A] extends PredefinedHeader[A]

  import HeaderValue._

  /**
   * General header fields.
   */
  object General {

    object `Cache-Control` extends General[String] with StringValue

    object `Connection` extends General[Array[String]] with TokenList

    object `Date` extends General[java.util.Date] with DateValue

    object `Pragma` extends General[String] with StringValue

    object `Trailer` extends General[String] with StringValue

    object `Transfer-Encoding` extends General[String] with StringValue

    object `Upgrade` extends General[String] with StringValue

    object `Via` extends General[String] with StringValue

    object `Warning` extends General[String] with StringValue

    object `X-Forwarded-For` extends General[String] with StringValue

  }

  /**
   * Request header fields.
   */
  object Request {

    object `Accept` extends Request[String] with StringValue

    object `Accept-Charset` extends Request[String] with StringValue

    object `Accept-Encoding` extends Request[String] with StringValue

    object `Accept-Language` extends Request[String] with StringValue

    object `Authorization` extends Request[String] with StringValue

    object `Expect` extends Request[String] with StringValue

    object `From` extends Request[String] with StringValue

    object `Host` extends Request[String] with StringValue

    object `If-Match` extends Request[String] with StringValue

    object `If-Modified-Since` extends Request[String] with StringValue

    object `If-None-Match` extends Request[String] with StringValue

    object `If-Range` extends Request[String] with StringValue

    object `If-Unmodified-Since` extends Request[String] with StringValue

    object `Max-Forwards` extends Request[String] with StringValue

    object `Proxy-Authorization` extends Request[String] with StringValue

    object `Range` extends Request[String] with StringValue

    object `Referer` extends Request[String] with StringValue

    object `TE` extends Request[String] with StringValue

    object `User-Agent` extends Request[String] with StringValue

  }

  /**
   * Entity header fields.
   */
  object Entity {

    object `Allow` extends Entity[String] with StringValue

    object `Content-Disposition` extends Entity[String] with StringValue

    object `Content-Encoding` extends Entity[String] with StringValue

    object `Content-Language` extends Entity[String] with StringValue

    object `Content-Length` extends Entity[Int] with IntValue

    object `Content-Location` extends Entity[String] with StringValue

    object `Content-MD5` extends Entity[String] with StringValue

    object `Content-Range` extends Entity[String] with StringValue

    object `Content-Type` extends Entity[ContentType] with ContentTypeValue

    object `Expires` extends Entity[String] with StringValue

    object `Last-Modified` extends Entity[java.util.Date] with DateValue

  }

  /**
   *  Response header fields.
   */
  object Response {

    object `Accept-Ranges` extends Response[String] with StringValue

    object `Age` extends Response[String] with StringValue

    object `ETag` extends Response[String] with StringValue

    object `Location` extends Response[String] with StringValue

    object `Proxy-Authenticate` extends Response[String] with StringValue

    object `Retry-After` extends Response[String] with StringValue

    object `Server` extends Response[String] with StringValue

    object `Vary` extends Response[String] with StringValue

    object `WWW-Authenticate` extends Response[String] with StringValue

  }
  /**
   * Non-predefined header fields.
   */
  class `User-Defined` private (val name: String) extends Header[String] with StringValue

  object `User-Defined` {

    def apply(name: String) = new `User-Defined`(name.toLowerCase)

  }

}
