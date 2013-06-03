package com.ibm

package plain

package http

import java.nio.ByteBuffer

import reflect.{ scalifiedName, subClasses }

import aio.Io
import aio.Renderable
import aio.Renderable._
import text.`US-ASCII`
import Status.ClientError.`415`

/**
 *
 */
abstract class MimeType

  extends Renderable {

  import MimeType._

  def name: String

  def extensions: Set[String]

  def text: Array[Byte]

  @inline final def render(implicit buffer: ByteBuffer) = r(text) + ^

}

/**
 * The MimeType object.
 */
object MimeType {

  def apply(name: String): MimeType = name.toLowerCase match {
    case "*/*" | "*" ⇒ `*/*`

    case "application/atom+xml" ⇒ `application/atom+xml`
    case "cgr" | "application/cgr" | "application/x-cgr" | "application/catia-3d" ⇒ `application/vnd.catiav5-cgr`
    case "3dxml" | "application/3dxml" | "application/x-3exml" | "application/vnd.catiav5-3dxml" ⇒ `application/vnd.catiav5-3dxml`
    case "application/catiav5-local2d" | "application/vnd-catiav5-drawing" ⇒ `application/vnd.catiav5-drawing`
    case "application/catiav5-part" | "application/vnd-catiav5-part" ⇒ `application/vnd.catiav5-part`
    case "model" | "application/model" | "application/x-model" | "application/vnd-catiav5-model" ⇒ `application/vnd.catiav5-model`
    case "application/catiav5-product" | "application/vnd-catiav5-product" ⇒ `application/vnd.catiav5-product`
    case "application/catiav5-material" | "application/vnd-catiav5-material" ⇒ `application/vnd.catiav5-material`
    case "application/catalog" | "application/vnd-catiav5-catalog" ⇒ `application/vnd.catiav5-catalog`
    case "application/catiav5-analysis" | "application/vnd-catiav5-analysis" ⇒ `application/vnd.catiav5-analysis`
    case "application/catiav5-process" | "application/vnd-catiav5-process" ⇒ `application/vnd.catiav5-process`
    case "jt" | "application/jt" | "application/x-jt" | "application/vnd-siemens-plm-jt" ⇒ `application/vnd.siemens-plm-jt`
    case "plmxml" | "application/plmxml" | "application/x-plmxml" | "application/unigraphics" | "application/vnd-siemens-plm-plmxml" ⇒ `application/vnd.siemens-plm-plmxml`
    case "vfz" | "application/vfz" | "application/x-vfz" | "application/vnd-siemens-plm-vfz" ⇒ `application/vnd.siemens-plm-vfz`
    case "application/gzip" | "application/x-gzip" ⇒ `application/gzip`
    case "application/javascript" ⇒ `application/javascript`
    case "application/x-jar" | "application/java-archive" ⇒ `application/java-archive`
    case "application/json" | "text/x-json" | "text/json" | "application/*" ⇒ `application/json`
    case "application/vnd.msexcel" | "application/msexcel" | "application.x-excel" ⇒ `application/vnd.msexcel`
    case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ⇒ `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
    case "application/vnd.mspowerpoint" | "application/mspowerpoint" | "application/x-mspowerpoint" ⇒ `application/vnd.mspowerpoint`
    case "application/vnd.openxmlformats-officedocument.presentationml.presentation" ⇒ `application/vnd.openxmlformats-officedocument.presentationml.presentation`
    case "application/vnd.msword" | "application/msword" | "application/x-msword" ⇒ `application/vnd.msword`
    case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ⇒ `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
    case "application/octet-stream" ⇒ `application/octet-stream`
    case "application/pdf" ⇒ `application/pdf`
    case "application/postscript" ⇒ `application/postscript`
    case "application/soap+xml" ⇒ `application/soap+xml`
    case "application/tar" | "application/x-tar" ⇒ `application/tar`
    case "application/xhtml+xml" ⇒ `application/xhtml+xml`
    case "application/xml" ⇒ `application/xml`
    case "application/xml+dtd" ⇒ `application/xml+dtd`
    case "application/tgz" | "application/x-tgz" ⇒ `application/x-tgz`
    case "application/x-7z-compressed" ⇒ `application/x-7z-compressed`
    case "application/x-javascript" ⇒ `application/x-javascript`
    case "application/x-shockwave-flash" ⇒ `application/x-shockwave-flash`
    case "application/x-www-form-urlencoded" ⇒ `application/x-www-form-urlencoded`
    case "application/x-scala-unit" ⇒ `application/x-scala-unit`
    case "application/zip" | "application/x-zip" ⇒ `application/zip`

    case "audio/basic" ⇒ `audio/basic`
    case "audio/mpeg" | "audio/*" ⇒ `audio/mpeg`
    case "audio/mp4" ⇒ `audio/mp4`
    case "audio/mp4a-latm" ⇒ `audio/mp4a-latm`

    case "image/gif" ⇒ `image/gif`
    case "image/png" | "image/x-png" | "image/*" ⇒ `image/png`
    case "image/jpeg" | "image/jpg" | "image/pjpeg" ⇒ `image/jpeg`
    case "image/svg+xml" ⇒ `image/svg+xml`
    case "image/tiff" | "application/raster" ⇒ `image/tiff`
    case "image/vnd.microsoft.icon" ⇒ `image/vnd.microsoft.icon`
    case "image/x-icon" ⇒ `image/x-icon`

    case "message/html" ⇒ `message/html`
    case "message/delivery-status" ⇒ `message/delivery-status`

    case "multipart/form-data" ⇒ `multipart/form-data`

    case "text/css" ⇒ `text/css`
    case "text/comma-separated-values" ⇒ `text/comma-separated-values`
    case "text/csv" ⇒ `text/csv`
    case "text/html" ⇒ `text/html`
    case "text/javascript" | "text/x-javascript" ⇒ `text/javascript`
    case "text/plain" | "text/*" ⇒ `text/plain`
    case "text/rtf" ⇒ `text/rtf`
    case "text/tab-separated-values" ⇒ `text/tab-separated-values`
    case "text/xml" ⇒ `text/xml`

    case "video/mpeg" ⇒ `video/mpeg`
    case "video/mp4" | "video/*" ⇒ `video/mp4`
    case "video/quicktime" ⇒ `video/quicktime`
    case "video/x-m4v" ⇒ `video/x-m4v`
    case "video/x-msvideo" ⇒ `video/x-msvideo`

    case other ⇒ `User-defined`(other)
  }

  /**
   *
   */
  abstract class PredefinedMimeType(ext: Seq[String] = Seq.empty) extends MimeType {

    final val name = toString

    final val extensions = ext.toSet

    final val text = name.getBytes(`US-ASCII`)

    extensions.foreach { e ⇒ extensionsmap = extensionsmap ++ Map(e -> this) }

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
  case object `application/vnd.catiav5-cgr` extends `application/*`("cgr")
  case object `application/vnd.catiav5-3dxml` extends `application/*`("3dxml")
  case object `application/vnd.catiav5-drawing` extends `application/*`("catdrawing")
  case object `application/vnd.catiav5-model` extends `application/*`("model")
  case object `application/vnd.catiav5-part` extends `application/*`("catpart")
  case object `application/vnd.catiav5-product` extends `application/*`("catproduct")
  case object `application/vnd.catiav5-material` extends `application/*`("catmaterial")
  case object `application/vnd.catiav5-catalog` extends `application/*`("catcatalog")
  case object `application/vnd.catiav5-analysis` extends `application/*`("catanalysis")
  case object `application/vnd.catiav5-process` extends `application/*`("catcatalog")
  case object `application/vnd.siemens-plm-jt` extends `application/*`("jt")
  case object `application/vnd.siemens-plm-plmxml` extends `application/*`("plmxml")
  case object `application/vnd.siemens-plm-vfz` extends `application/*`("vfz")
  case object `application/gzip` extends `application/*`("gz", "gzip")
  case object `application/javascript` extends `application/*`("js")
  case object `application/java-archive` extends `application/*`("jar")
  case object `application/json` extends `application/*`("json")
  case object `application/vnd.msexcel` extends `application/*`("xls")
  case object `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` extends `application/*`("xlsx")
  case object `application/vnd.mspowerpoint` extends `application/*`("ppt")
  case object `application/vnd.openxmlformats-officedocument.presentationml.presentation` extends `application/*`("pptx")
  case object `application/vnd.msword` extends `application/*`("doc")
  case object `application/vnd.openxmlformats-officedocument.wordprocessingml.document` extends `application/*`("docx")
  case object `application/octet-stream` extends `application/*`("bin", "class", "exe", "com", "dll", "lib", "a", "o")
  case object `application/pdf` extends `application/*`("pdf")
  case object `application/postscript` extends `application/*`("ps", "ai")
  case object `application/soap+xml` extends `application/*`("xml")
  case object `application/tar` extends `application/*`("tar")
  case object `application/xhtml+xml` extends `application/*`("xml")
  case object `application/xml` extends `application/*`("xml")
  case object `application/xml+dtd` extends `application/*`("xml", "dtd")
  case object `application/x-7z-compressed` extends `application/*`("7z")
  case object `application/x-tgz` extends `application/*`("tgz")
  case object `application/x-javascript` extends `application/*`("js")
  case object `application/x-shockwave-flash` extends `application/*`("swf")
  case object `application/x-www-form-urlencoded` extends `application/*`
  case object `application/x-scala-unit` extends `application/*`
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
  case object `text/plain` extends `text/*`("txt", "text", "ini", "log", "conf", "md", "properties", "plain", "bat", "cmd")
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

    final val text = name.getBytes(`US-ASCII`)

  }

  object `User-defined` {

    def apply(name: String) = new `User-defined`(name.toLowerCase)

  }

  def forExtension(extension: String): Option[MimeType] = extensionsmap.get(extension)

  private[this] var extensionsmap: scala.collection.immutable.SortedMap[String, MimeType] = scala.collection.immutable.SortedMap.empty

  subClasses(classOf[MimeType]).map(scalifiedName).filter(!_.endsWith("MimeType")).foreach(apply)

}

