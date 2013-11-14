package com.ibm

package plain

package servlet

package http

import java.io.{ ByteArrayOutputStream, OutputStreamWriter, PrintWriter }
import java.util.{ Collection, Locale }

import io.ServletOutputStream
import javax.{ servlet ⇒ js }
import plain.http.{ ContentType, Entity }
import plain.http.Entity.ArrayEntity
import plain.http.Response

final case class HttpServletResponse(

  private final val response: Response,

  private final val servletcontext: ServletContext)

  extends js.http.HttpServletResponse {

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

  final def getHeaderNames: Collection[String] = unsupported

  final def getHeaders(x$1: String): Collection[String] = unsupported

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

  final def flushBuffer = if (usewriter) printwriter.flush else if (useoutputstream) outputstream.flush

  final def getBufferSize = buffersize

  final def getCharacterEncoding: String = unsupported

  final def getContentType: String = unsupported

  final def getLocale: Locale = unsupported

  final def getOutputStream: js.ServletOutputStream = { if (usewriter) throw new IllegalStateException; useoutputstream = true; new ServletOutputStream(outputstream) }

  final def getWriter: PrintWriter = { if (useoutputstream) throw new IllegalStateException; usewriter = true; printwriter }

  final def isCommitted: Boolean = unsupported

  final def reset = { usewriter = false; useoutputstream = false }

  final def resetBuffer = {
    setBufferSize(defaultbuffersize)
    reset
  }

  final def setBufferSize(buffersize: Int) = {
    this.buffersize = buffersize
    outputstream = new ByteArrayOutputStream(buffersize)
    printwriter = new PrintWriter(new OutputStreamWriter(outputstream))
  }

  final def setCharacterEncoding(x$1: String) = unsupported

  final def setContentLength(x$1: Int) = unsupported

  final def setContentLengthLong(x$1: Long) = unsupported

  final def setContentType(x$1: String) = unsupported

  final def setLocale(x$1: java.util.Locale) = unsupported

  final def getEntity: Entity = { flushBuffer; ArrayEntity(outputstream.toByteArray, contenttype) }

  private[this] final val defaultbuffersize = 128

  private[this] final var buffersize = defaultbuffersize

  private[this] final var outputstream = new ByteArrayOutputStream(defaultbuffersize)

  private[this] final var printwriter = new PrintWriter(new OutputStreamWriter(outputstream))

  private[this] final var usewriter = false

  private[this] final var useoutputstream = false

  private[this] final var contenttype: ContentType = null

}

