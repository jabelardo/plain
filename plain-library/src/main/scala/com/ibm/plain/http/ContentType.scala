package com.ibm

package plain

package http

import java.nio.charset.Charset

import language.implicitConversions

import aio.{ Io, Renderable }
import aio.Renderable.r
import text.{ fastSplit, `ISO-8859-15`, `UTF-8` }
import Status.ClientError
import MimeType.`application/json`

/**
 *
 */
final case class ContentType private (

  mimetype: MimeType,

  charset: Option[Charset])

  extends Renderable {

  import ContentType._

  @inline def charsetOrDefault = charset match { case Some(charset) ⇒ charset case None ⇒ `ISO-8859-15` }

  @inline final def render(implicit io: Io) = mimetype + r(charset match { case None ⇒ "" case Some(c) ⇒ "; charset=" + c.displayName })

}

/**
 * The ContentType object.
 */
object ContentType {

  def apply(mimetype: MimeType) = mimetype match {
    case `application/json` ⇒ new ContentType(mimetype, Some(`UTF-8`))
    case mimetype ⇒ new ContentType(mimetype, None)
  }

  /**
   * Given a header value like "text/html; charset=ISO-8859-15" this will split it into a MimeType and a Charset.
   */
  def apply(headervalue: String): ContentType = fastSplit(headervalue, ';') match {
    case mimetype :: Nil ⇒ apply(MimeType(mimetype))
    case mimetype :: charset :: Nil ⇒
      apply(MimeType(mimetype.trim), Some(try Charset.forName(charset.trim.replace("charset=", "")) catch { case _: Throwable ⇒ `ISO-8859-15` }))
    case _ ⇒ throw ClientError.`415`
  }

  @inline implicit def fromMimeType(mimetype: MimeType) = ContentType(mimetype)

}

trait ContentTypeValue extends HeaderValue[ContentType] {

  @inline def value(name: String) = ContentType(name)

}
