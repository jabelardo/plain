package com.ibm

package plain

package http

import java.nio.ByteBuffer
import java.nio.charset.Charset

import language.implicitConversions

import aio.Renderable
import aio.Renderable._
import text.{ fastSplit, `UTF-8` }
import Status.ClientError
import MimeType._

/**
 *
 */
final case class ContentType private (

  mimetype: MimeType,

  charset: Option[Charset])

    extends Renderable {

  import ContentType._

  override final def toString = new String(text)

  @inline final def charsetOrDefault = charset match { case Some(charset) ⇒ charset case None ⇒ defaultCharacterSet }

  @inline final def render(implicit buffer: ByteBuffer) = r(text) + ^

  private[this] final val text = (charset match { case None ⇒ mimetype.toString case Some(c) ⇒ mimetype + "; charset=" + c.displayName }).getBytes

}

/**
 * The ContentType object.
 */
object ContentType {

  final def apply(mimetype: MimeType) = mimetype match {
    case `application/json` | `application/xml` | `text/xml` | `text/html` | `text/plain` ⇒ new ContentType(mimetype, Some(`UTF-8`))
    case mimetype ⇒ new ContentType(mimetype, None)
  }

  final def apply(mimetype: MimeType, charset: Charset) = new ContentType(mimetype, Some(charset))

  /**
   * Given a header value like "text/html; charset=ISO-8859-15" this will split it into a MimeType and a Charset.
   */
  final def apply(headervalue: String): ContentType = fastSplit(headervalue, ';') match {
    case mimetype :: Nil ⇒ apply(MimeType(mimetype))
    case mimetype :: charset :: Nil ⇒
      apply(MimeType(mimetype.trim), Some(try Charset.forName(charset.trim.replace("charset=", "")) catch { case _: Throwable ⇒ defaultCharacterSet }))
    case _ ⇒ throw ClientError.`415`
  }

  final implicit def fromMimeType(mimetype: MimeType) = ContentType(mimetype)

  final val default = apply(`application/json`)

}

trait ContentTypeValue extends HeaderValue[ContentType] {

  @inline def value(name: String) = ContentType(name)

}
