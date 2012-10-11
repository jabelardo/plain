package com.ibm.plain

package lib

package http

/**
 * A simple HttpHeader case class hierarchy.
 */
sealed abstract class HttpHeader {

  val name: String

  val value: String

  override def equals(other: Any) = try {
    other.asInstanceOf[HttpHeader].name.equalsIgnoreCase(name)
  } catch {
    case _: Throwable ⇒ false
  }

  override def hashCode = name.toUpperCase.hashCode

}

abstract sealed class PredefinedHttpHeader extends HttpHeader {

  val name = getClass.getSimpleName

}

abstract sealed class RequestHttpHeader extends PredefinedHttpHeader

abstract sealed class ResponseHttpHeader extends PredefinedHttpHeader

case class `User-Defined`(name: String, value: String) extends HttpHeader

/**
 * Predefined request headers, they can contain header specific logic and behavior.
 */

case class `Accept`(value: String) extends RequestHttpHeader

case class `Accept-Charset`(value: String) extends RequestHttpHeader

case class `Accept-Encoding`(value: String) extends RequestHttpHeader

case class `Accept-Language`(value: String) extends RequestHttpHeader

case class `Connection`(value: String) extends RequestHttpHeader {

  val isKeepAlive = "keep-alive".equalsIgnoreCase(value)

  val isClose = "close".equalsIgnoreCase(value)

}

case class `Host`(value: String) extends RequestHttpHeader

case class `User-Agent`(value: String) extends RequestHttpHeader

object HttpHeader {

  def apply(name: String, value: String): HttpHeader = name.toLowerCase match {
    case "accept" ⇒ `Accept`(value)
    case "accept-charset" ⇒ `Accept-Charset`(value)
    case "accept-encoding" ⇒ `Accept-Encoding`(value)
    case "accept-language" ⇒ `Accept-Language`(value)
    case "connection" ⇒ `Connection`(value)
    case "host" ⇒ `Host`(value)
    case "user-agent" ⇒ `User-Agent`(value)
    case _ ⇒ `User-Defined`(name, value)
  }

  def test = {
    apply("Connection", "keep-alive") match {
      case h @ `Accept`(_) ⇒ println(h)
      case h @ `Connection`(_) ⇒ println(h.isKeepAlive)
      case h @ `User-Defined`(name, value) ⇒ println(name + " " + value)
      case h ⇒ println(h)
    }
  }
}

