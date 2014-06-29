package com.ibm

package plain

package servlet

package http

import java.nio.charset.Charset
import java.util.{ Collection, Locale }

import javax.{ servlet ⇒ js }

import scala.collection.mutable.{ Map ⇒ MutableMap }

import plain.http.{ ContentType, Entity }
import plain.http.{ MimeType, Response, Status }
import plain.http.Entity.ArrayEntity
import plain.io.PrintWriter

final class HttpServletResponse(

  private[this] final val response: Response,

  private[this] final val servletcontext: ServletContext,

  private[this] final val printwriter: PrintWriter,

  private[this] final val servlet: js.http.HttpServlet)

  extends js.http.HttpServletResponse {

  final def addCookie(cookie: js.http.Cookie) = response ++ cookie

  final def addDateHeader(name: String, value: Long) = addHeader(name, value.toString)

  final def addHeader(name: String, value: String) = setHeader(name, value)

  final def addIntHeader(name: String, value: Int) = addHeader(name, value.toString)

  final def containsHeader(name: String): Boolean = response.headers.contains(name)

  final def encodeRedirectURL(x$1: String): String = unsupported

  final def encodeRedirectUrl(x$1: String): String = unsupported

  final def encodeURL(x$1: String): String = unsupported

  final def encodeUrl(x$1: String): String = unsupported

  final def getHeader(x$1: String): String = unsupported

  final def getHeaderNames: Collection[String] = unsupported

  final def getHeaders(x$1: String): Collection[String] = unsupported

  final def getStatus: Int = response.status.code.toInt

  final def sendError(code: Int) = sendError(code, "")

  final def sendError(code: Int, msg: String) = {
    log(if (0 < msg.length) msg else "<>", Status(code))
    throw Status(code)
  }

  final def sendRedirect(redirect: String) = redirect match {
    case r if r == servletcontext.getContextPath + "/" ⇒
      log(r, Status.Redirection.`302`); throw Status.Redirection.`301`
    case r ⇒
      log(r, Status.Redirection.`307`); throw Status.Redirection.`307`
  }

  final def setDateHeader(name: String, value: Long) = setHeader(name, value.toString)

  final def setHeader(name: String, value: String) = name match {
    case "Content-Type" ⇒ contenttype = ContentType(value)
    case _              ⇒ response.headers.asInstanceOf[MutableMap[String, String]].put(name, value)
  }

  final def setIntHeader(name: String, value: Int) = setHeader(name, value.toString)

  final def setStatus(code: Int, message: String) = setStatus(code)

  final def setStatus(code: Int) = response ++ Status(code)

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

  final def log(msg: String, e: Throwable) = servletcontext.log(msg, e)

  private[this] final var usewriter = false

  private[this] final var useoutputstream = false

  private[this] final var contenttype = ContentType(MimeType.`text/plain`)

  private[this] final var characterencoding = text.`UTF-8`

}

