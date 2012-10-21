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

  def apply(name: String): ContentType = name.toLowerCase match {
    case "text/plain" ⇒ `text/plain`
    case _ ⇒ null // :TODO:
  }

  /**
   *
   */
  abstract sealed class PredefinedContentType extends ContentType {

    final val mimetype = MimeType(reflect.simpleName(getClass))

    final val charset: Option[Charset] = None

  }

  case object `text/plain` extends PredefinedContentType

  case object `text/xml` extends PredefinedContentType

  case object `application/xml` extends PredefinedContentType

  case object `application/json` extends PredefinedContentType

  case object `application/octet-stream` extends PredefinedContentType

  case class `User-defined`(private val mtype: String, private val cset: String) extends ContentType {

    val mimetype = MimeType(mtype)

    val charset = try { Some(Charset.forName(cset)) } catch { case _: Throwable ⇒ None }

  }

}

