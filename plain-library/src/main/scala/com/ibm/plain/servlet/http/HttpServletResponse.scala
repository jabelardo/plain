package com.ibm

package plain

package servlet

package http

import java.io.{ ByteArrayOutputStream, OutputStreamWriter, PrintWriter }

import io.ServletOutputStream
import javax.servlet.{ ServletOutputStream ⇒ JServletOutputStream }
import javax.servlet.http.{ HttpServletResponse ⇒ JHttpServletResponse }
import plain.http.{ ContentType, Entity }
import plain.http.Entity.ArrayEntity
import plain.http.Response

final case class HttpServletResponse(

  response: Response)

  extends JHttpServletResponse {

  final def addCookie(x$1: javax.servlet.http.Cookie) = unsupported

  final def addDateHeader(x$1: String, x$2: Long) = unsupported

  final def addHeader(x$1: String, x$2: String) = unsupported

  final def addIntHeader(x$1: String, x$2: Int) = unsupported

  final def containsHeader(x$1: String): Boolean = unsupported

  final def encodeRedirectURL(x$1: String): String = unsupported

  final def encodeRedirectUrl(x$1: String): String = unsupported

  final def encodeURL(x$1: String): String = unsupported

  final def encodeUrl(x$1: String): String = unsupported

  final def getHeader(x$1: String): String = unsupported

  final def getHeaderNames: java.util.Collection[String] = unsupported

  final def getHeaders(x$1: String): java.util.Collection[String] = unsupported

  final def getStatus: Int = unsupported

  final def sendError(x$1: Int) = unsupported

  final def sendError(x$1: Int, x$2: String) = unsupported

  final def sendRedirect(x$1: String) = unsupported

  final def setDateHeader(x$1: String, x$2: Long) = unsupported

  final def setHeader(name: String, value: String) = name match {
    case "Content-Type" ⇒ contenttype = ContentType(value)
    case _ ⇒ println(name + " : " + value); unsupported
  }

  final def setIntHeader(x$1: String, x$2: Int) = unsupported

  final def setStatus(x$1: Int, x$2: String) = unsupported

  final def setStatus(x$1: Int) = unsupported

  final def flushBuffer = unsupported

  final def getBufferSize: Int = unsupported

  final def getCharacterEncoding: String = unsupported

  final def getContentType: String = unsupported

  final def getLocale: java.util.Locale = unsupported

  final def getOutputStream: JServletOutputStream = { if (usewriter) throw new IllegalStateException; useoutputstream = true; new ServletOutputStream(outputstream) }

  final def getWriter: PrintWriter = { if (useoutputstream) throw new IllegalStateException; usewriter = true; printwriter }

  final def isCommitted: Boolean = unsupported

  final def reset = { usewriter = false; useoutputstream = false }

  final def resetBuffer = unsupported

  final def setBufferSize(x$1: Int) = unsupported

  final def setCharacterEncoding(x$1: String) = unsupported

  final def setContentLength(x$1: Int) = unsupported

  final def setContentLengthLong(x$1: Long) = unsupported

  final def setContentType(x$1: String) = unsupported

  final def setLocale(x$1: java.util.Locale) = unsupported

  final def getEntity: Entity = { if (usewriter) printwriter.flush; if (useoutputstream) outputstream.flush; ArrayEntity(outputstream.toByteArray, contenttype) }

  private[this] final var contenttype: ContentType = null

  private[this] final val outputstream = new ByteArrayOutputStream(1024)

  private[this] final val printwriter = new PrintWriter(new OutputStreamWriter(outputstream))

  private[this] final var usewriter = false

  private[this] final var useoutputstream = false

}


