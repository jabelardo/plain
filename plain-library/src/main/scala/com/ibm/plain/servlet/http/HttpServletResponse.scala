package com.ibm

package plain

package servlet

package http

import java.nio.charset.Charset
import java.util.{ Collection, Locale }

import javax.{ servlet ⇒ js }

import scala.collection.mutable.{ Map ⇒ MutableMap }

import com.ibm.plain.servlet.ServletOutputStream
import plain.http.{ ContentType, Entity }
import plain.http.{ MimeType, Response, Status }
import plain.http.Entity.ArrayEntity
import plain.io.PrintWriter

final class HttpServletResponse(

  private[this] final val response: Response,

  private[this] final val servletcontext: ServletContext,

  private[this] final val printwriter: PrintWriter)

  extends js.http.HttpServletResponse

  with logging.HasLogger {

  final def addCookie(cookie: js.http.Cookie) = response ++ cookie

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

  final def getStatus: Int = response.status.code.toInt

  final def sendError(x$1: Int) = unsupported

  final def sendError(x$1: Int, x$2: String) = unsupported

  final def sendRedirect(redirect: String) = {
    dumpStack; println("sendRedirect " + redirect)
    response ++ Status.Redirection.`302`
    setHeader("Location", "http://google.de")
  }

  final def setDateHeader(name: String, value: Long) = setHeader(name, value.toString)

  final def setHeader(name: String, value: String) = name match {
    case "Content-Type" ⇒ contenttype = ContentType(value)
    case _ ⇒ response.headers.asInstanceOf[MutableMap[String, String]].put(name, value)
  }

  final def setIntHeader(x$1: String, x$2: Int) = unsupported

  final def setStatus(x$1: Int, x$2: String) = unsupported

  final def setStatus(status: Int) = response ++ Status(status)

  final def flushBuffer = printwriter.flush

  final def getBufferSize = printwriter.outputstream.getCapactiy

  final def getCharacterEncoding: String = characterencoding.toString

  final def getContentType: String = contenttype.toString

  final def getLocale: Locale = unsupported

  final def getOutputStream: js.ServletOutputStream = { if (usewriter) throw new IllegalStateException; useoutputstream = true; new ServletOutputStream(printwriter.outputstream) }

  final def getWriter: PrintWriter = { if (useoutputstream) throw new IllegalStateException; usewriter = true; printwriter }

  final def isCommitted: Boolean = unsupported

  final def reset = { usewriter = false; useoutputstream = false }

  final def resetBuffer = {
    printwriter.outputstream.reset
    reset
  }

  final def setBufferSize(buffersize: Int) = printwriter.outputstream.setCapacity(buffersize)

  final def setCharacterEncoding(encoding: String) = {
    characterencoding = Charset.forName(encoding)
    printwriter.setCharacterSet(characterencoding)
  }

  final def setContentLength(length: Int) = setHeader("Content-Length", length.toString)

  final def setContentLengthLong(length: Long) = setHeader("Content-Type", length.toString)

  final def setContentType(contenttype: String) = setHeader("Content-Type", contenttype)

  final def setLocale(x$1: java.util.Locale) = unsupported

  final def getEntity: Entity = ArrayEntity(printwriter.outputstream.getArray, 0, printwriter.outputstream.length, contenttype)

  private[this] final var usewriter = false

  private[this] final var useoutputstream = false

  private[this] final var contenttype = ContentType(MimeType.`text/plain`)

  private[this] final var characterencoding = text.`UTF-8`

}

