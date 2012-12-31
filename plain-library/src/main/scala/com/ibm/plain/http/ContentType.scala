package com.ibm

package plain

package http

import java.nio.charset.Charset

import Status.ClientError
import aio.{ Io, Renderable }
import aio.Renderable.r
import text.{ fastSplit, `ISO-8859-15` }

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

  @inline override def equals(other: Any) = other match {
    case b: ContentType ⇒ mimetype == b.mimetype
    case _ ⇒ false
  }

  @inline override def hashCode = mimetype.hashCode

}

/**
 * The ContentType object.
 */
object ContentType {

  def apply(mimetype: MimeType) = new ContentType(mimetype, None)

  /**
   * Given a header value like "text/html; charset=ISO-8859-4" this will split it into a MimeType and a Charset.
   */
  def apply(headervalue: String): ContentType = fastSplit(headervalue, ';') match {
    case mimetype :: Nil ⇒ apply(MimeType(mimetype))
    case mimetype :: charset :: Nil ⇒ apply(MimeType(mimetype.trim), Some(try Charset.forName(charset.trim.replace("charset=", "")) catch { case _: Throwable ⇒ Charset.defaultCharset }))
    case _ ⇒ throw ClientError.`415`
  }

}

trait ContentTypeValue extends HeaderValue[ContentType] {

  @inline def value(name: String) = ContentType(name)

}
