package com.ibm

package plain

package http

import java.nio.ByteBuffer

import aio.Io
import aio.Renderable
import aio.Renderable.r
import Status.ClientError.`415`

/**
 *
 */
abstract class MimeType

  extends Renderable {

  def name: String

  def extensions: Set[String]

  @inline final def render(implicit io: Io) = r(name)

}

/**
 * The MimeType object.
 */
object MimeType {

  def apply(name: String): MimeType = name.toLowerCase match {
    case "*/*" ⇒ `*/*`
    case "audio/mp4" ⇒ `audio/mp4`
    case "text/plain" ⇒ `text/plain`
    // more you lazy ...
    case n ⇒ `User-defined`(n) // :TODO:
  }

  /**
   *
   */
  abstract class PredefinedMimeType(ext: Seq[String] = Seq.empty) extends MimeType {

    final val name = toString

    final val extensions = ext.toSet

  }

  abstract class `audio/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `application/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `image/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `message/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `multipart/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `text/*`(ext: String*) extends PredefinedMimeType(ext)
  abstract class `video/*`(ext: String*) extends PredefinedMimeType(ext)

  case object `*/*` extends PredefinedMimeType

  case object `application/atom+xml` extends `application/*`("xml")
  case object `application/gzip` extends `application/*`("gz")
  case object `application/javascript` extends `application/*`("js")
  case object `application/json` extends `application/*`("json")
  case object `application/msexcel` extends `application/*`("xls")
  case object `application/mspowerpoint` extends `application/*`("ppt")
  case object `application/msword` extends `application/*`("doc")
  case object `application/octet-stream` extends `application/*`("bin", "class", "exe", "com", "dll", "lib", "a", "o")
  case object `application/pdf` extends `application/*`("pdf")
  case object `application/postscript` extends `application/*`("ps", "ai")
  case object `application/soap+xml` extends `application/*`("xml")
  case object `application/xhtml+xml` extends `application/*`("xml")
  case object `application/xml` extends `application/*`("xml")
  case object `application/xml+dtd` extends `application/*`("xml", "dtd")
  case object `application/x-7z-compressed` extends `application/*`("7z")
  case object `application/x-javascript` extends `application/*`("js")
  case object `application/x-shockwave-flash` extends `application/*`("swf")
  case object `application/x-www-form-urlencoded` extends `application/*`
  case object `application/zip` extends `application/*`("zip")

  case object `audio/basic` extends `audio/*`("au", "snd")
  case object `audio/mpeg` extends `audio/*`("mpeg", "mpg", "mp2", "mp3", "mpe", "mpga")
  case object `audio/mp4` extends `audio/*`("mp4")
  case object `audio/mp4a-latm` extends `audio/*`("m4a")

  case object `image/gif` extends `image/*`("gif")
  case object `image/png` extends `image/*`("png")
  case object `image/jpeg` extends `image/*`("jpeg", "jpg", "jpe")
  case object `image/svg+xml` extends `image/*`("svg")
  case object `image/tiff` extends `image/*`("tiff", "tif")
  case object `image/vnd.microsoft.icon` extends `image/*`("icon", "ico")
  case object `image/x-icon` extends `image/*`("icon", "ico")

  case object `message/html` extends `message/*`
  case object `message/delivery-status` extends `message/*`

  case object `multipart/form-data` extends `multipart/*`

  case object `text/css` extends `text/*`("css")
  case object `text/comma-separated-values` extends `text/*`("csv")
  case object `text/csv` extends `text/*`("csv")
  case object `text/html` extends `text/*`("html", "html")
  case object `text/javascript` extends `text/*`("js")
  case object `text/plain` extends `text/*`("txt", "text", "ini", "log", "conf", "properties", "plain", "bat", "cmd")
  case object `text/rtf` extends `text/*`("rtf")
  case object `text/tab-separated-values` extends `text/*`("tsv")
  case object `text/xml` extends `text/*`("xml")

  case object `video/mpeg` extends `video/*`("mpeg", "mpg", "mpe")
  case object `video/mp4` extends `video/*`("mp4")
  case object `video/quicktime` extends `video/*`("qt", "mov")
  case object `video/x-m4v` extends `video/*`("m4v")
  case object `video/x-msvideo` extends `video/*`("avi")

  /**
   * Non-predefined mime type.
   */
  class `User-defined` private (val name: String)

    extends MimeType {

    val extensions: Set[String] = Set.empty

    if (1 != (name.size - name.replace("/", "").size)) throw `415`

    override final val toString = name

  }

  object `User-defined` {

    def apply(name: String) = new `User-defined`(name.toLowerCase)

  }

}

