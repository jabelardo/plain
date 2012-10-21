package com.ibm.plain

package lib

package http

import java.nio.charset.Charset

/**
 *
 */
abstract sealed class ContentType {

  val mimetype: MimeType

  val charset: Option[Charset]

  final def render = mimetype + "; charset=" + charset

}

/**
 * The ContentType object.
 */
object ContentType {

  /**
   *
   */
  abstract sealed class PredefinedContentType extends ContentType {

    final val mimetype = MimeType(toString)

    final val charset: Option[Charset] = None

  }

  case object `text/plain` extends PredefinedContentType

  case object `text/xml` extends PredefinedContentType

  case object `application/xml` extends PredefinedContentType

  case object `application/json` extends PredefinedContentType

  case object `application/octet-stream` extends PredefinedContentType

  case class `User-defined`(mimetype: MimeType, charset: Option[Charset]) extends ContentType

}

trait ContentTypeValue extends Header.Value[ContentType] {

  import ContentType._

  def value(name: String): ContentType = name match {
    case "text/plain" ⇒ `text/plain`
    case "text/xml" ⇒ `text/xml`
    case "application/xml" ⇒ `application/xml`
    case "application/json" ⇒ `application/json`
    case "application/octet-stream" ⇒ `application/octet-stream`
    case userdefined ⇒ `User-defined`(MimeType(userdefined), None)
  }

}
