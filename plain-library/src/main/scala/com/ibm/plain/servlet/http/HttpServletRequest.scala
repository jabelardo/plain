package com.ibm

package plain

package servlet

package http

import java.io.{ BufferedReader, PrintWriter, InputStreamReader }
import java.util.{ Enumeration, Locale, Map ⇒ JMap }
import javax.{ servlet ⇒ js }
import scala.collection.JavaConversions.{ asJavaEnumeration, mapAsJavaMap, mapAsScalaMap }
import scala.collection.mutable.HashMap
import aio.Exchange
import text.`UTF-8`
import rest.Context
import rest.Matching.default.decodeForm
import plain.http.MimeType.`text/plain`
import plain.http.Request
import plain.http.Header.Request.`Cookie`
import plain.http.Entity.ArrayEntity
import plain.io.ByteArrayInputStream
import scala.collection.convert.Wrappers.ToIteratorWrapper

final class HttpServletRequest(

  private[this] final val request: Request,

  private[this] final val exchange: Exchange[Context],

  private[this] final val servletcontext: ServletContext,

  private[this] final val servlet: js.http.HttpServlet)

  extends js.http.HttpServletRequest

  with HasAttributes {

  final def authenticate(x$1: js.http.HttpServletResponse): Boolean = unsupported

  final def changeSessionId: String = unsupported

  final def getAuthType: String = unsupported

  final def getContextPath: String = contextpath

  final def getCookies: Array[js.http.Cookie] = unsupported

  final def getDateHeader(x$1: String): Long = unsupported

  final def getIntHeader(name: String): Int = ignoreOrElse(getHeader(name).toInt, -1)

  final val getMethod: String = request.method.toString

  final def getPart(x$1: String): js.http.Part = unsupported

  final def getParts: java.util.Collection[js.http.Part] = unsupported

  final def getPathInfo: String = pathinfo

  final def getPathTranslated: String = null

  final def getQueryString: String = request.query.getOrElse(null)

  final def getRemoteUser: String = unsupported

  final def getRequestURI: String = requesturi

  final def getRequestURL: StringBuffer = new StringBuffer(requesturi)

  final def getRequestedSessionId: String = `Cookie`(request.headers) match {
    case Some(value) ⇒ value.split(";").head.split("=")(1)
    case _ ⇒ null
  }

  final def getServletPath: String = servletpath

  final def getSession: js.http.HttpSession = httpsession

  final def getSession(create: Boolean): js.http.HttpSession = getSession

  final def getUserPrincipal: java.security.Principal = null

  final def isRequestedSessionIdFromCookie: Boolean = unsupported

  final def isRequestedSessionIdFromURL: Boolean = unsupported

  final def isRequestedSessionIdFromUrl: Boolean = unsupported

  final def isRequestedSessionIdValid: Boolean = unsupported

  final def isUserInRole(x$1: String): Boolean = unsupported

  final def login(x$1: String, x$2: String) = unsupported

  final def logout = unsupported

  final def upgrade[T <: js.http.HttpUpgradeHandler](x$1: Class[T]): T = unsupported

  final def getCharacterEncoding: String = getHeader("character-encoding")

  final def setCharacterEncoding(arg0: String) = unsupported

  final def getContentLength: Int = getIntHeader("content-length")

  final def getContentLengthLong: Long = getHeader("content-length").toLong

  final def getContentType: String = getHeader("content-type")

  final def getInputStream: js.ServletInputStream = request.entity match {
    case Some(arrayentity: ArrayEntity) ⇒ new ServletInputStream(new io.ByteArrayInputStream(arrayentity.array, arrayentity.offset, arrayentity.length.toInt))
    case _ ⇒ unsupported
  }

  final def getParameter(name: String): String = getParameterMap.get(name) match { case null ⇒ null case values ⇒ values.head }

  final def getParameterNames: Enumeration[String] = getParameterMap.keysIterator

  final def getParameterValues(name: String): Array[String] = getParameterMap.get(name)

  final def getParameterMap: JMap[String, Array[String]] = parametermap

  final def getProtocol: String = request.version.toString

  final def getScheme: String = unsupported

  final def getServerName: String = getLocalName

  final def getServerPort: Int = getLocalPort

  final def getReader: BufferedReader = new BufferedReader(new InputStreamReader(getInputStream))

  final def getRemoteAddr: String = remoteaddress.getHostString

  final def getRemoteHost: String = remoteaddress.getHostName

  final def getRemotePort: Int = remoteaddress.getPort

  final def getLocale: Locale = Locale.getDefault

  final def getLocales: Enumeration[Locale] = List(getLocale).toIterator

  final def isSecure: Boolean = false

  final def getRequestDispatcher(path: String): js.RequestDispatcher = new RequestDispatcher(path, servletcontext.asInstanceOf[ServletContext])

  final def getRealPath(path: String): String = servletcontext.getRealPath(path)

  final def getLocalName: String = localaddress.getHostName

  final def getLocalAddr: String = localaddress.getHostString

  final def getLocalPort: Int = localaddress.getPort

  final def getServletContext: js.ServletContext = servletcontext

  final def startAsync: js.AsyncContext = unsupported

  final def startAsync(arg0: js.ServletRequest, arg1: js.ServletResponse): js.AsyncContext = unsupported

  final def isAsyncStarted: Boolean = unsupported

  final def isAsyncSupported: Boolean = unsupported

  final def getAsyncContext: js.AsyncContext = unsupported

  final def getDispatcherType: js.DispatcherType = unsupported

  final def addCookie(cookie: js.http.Cookie) = unsupported

  final def containsHeader(arg0: String): Boolean = unsupported

  final def encodeURL(arg0: String): String = unsupported

  final def encodeRedirectURL(arg0: String): String = unsupported

  final def encodeUrl(arg0: String): String = unsupported

  final def encodeRedirectUrl(arg0: String): String = unsupported

  final def sendError(arg0: Int, arg1: String) = unsupported

  final def sendError(arg0: Int) = unsupported

  final def sendRedirect(arg0: String) = unsupported

  final def setDateHeader(arg0: String, arg1: Long) = unsupported

  final def addDateHeader(arg0: String, arg1: Long) = unsupported

  final def setHeader(name: String, value: String) = unsupported

  final def addHeader(arg0: String, arg1: String) = unsupported

  final def setIntHeader(arg0: String, arg1: Int) = unsupported

  final def addIntHeader(arg0: String, arg1: Int) = unsupported

  final def setStatus(arg0: Int) = unsupported

  final def setStatus(arg0: Int, arg1: String) = unsupported

  final def getStatus: Int = unsupported

  final def getHeader(name: String): String = request.headers.get(name) match {
    case Some(value) ⇒ value
    case _ ⇒ servletcontext.trace("Header not found : " + name); null
  }

  final def getHeaders(name: String): Enumeration[String] = List(getHeader(name)).toIterator

  final def getHeaderNames: Enumeration[String] = request.headers.keysIterator

  final def getOutputStream: js.ServletOutputStream = unsupported

  final def getWriter: PrintWriter = unsupported

  final def setContentLength(arg0: Int) = unsupported

  final def setContentLengthLong(arg0: Long) = unsupported

  final def setContentType(arg0: String) = unsupported

  final def setBufferSize(arg0: Int) = unsupported

  final def getBufferSize: Int = unsupported

  final def flushBuffer = unsupported

  final def resetBuffer = unsupported

  final def isCommitted: Boolean = unsupported

  final def reset = unsupported

  final def setLocale(arg0: java.util.Locale) = unsupported

  final def hasSession = hassession

  final lazy val requesturi = request.path.mkString("/", "/", "")

  final lazy val contextpath = servletcontext.getContextPath

  final lazy val servletpath = if (null == servlet.getServletConfig) "" else servletcontext.getServletMappings.getOrElse(servlet.getServletName, "") match {
    case "" ⇒ contextpath
    case s ⇒ s
  }

  final lazy val pathinfo = (requesturi.replace(contextpath, "").replace(servletpath, "") + "/") match { case "/" ⇒ null case i ⇒ i }

  final lazy val httpsession: HttpSession = {
    hassession = true
    `Cookie`(request.headers) match {
      case Some(value) ⇒
        HttpSession.retrieve(value.split(";").head.split("=")(1)) match {
          case null ⇒ HttpSession.create(crypt.Uuid.newUuid, servletcontext)
          case session ⇒ session
        }
      case _ ⇒
        HttpSession.create(crypt.Uuid.newUuid, servletcontext)
    }
  }

  final var hassession = false

  final lazy val parametermap: JMap[String, Array[String]] = if (request.query.isDefined) ignoreOrElse(
    decodeForm(Some(ArrayEntity(request.query.get.getBytes(`UTF-8`), `text/plain`))) map {
      case (name, values) ⇒ (name, values.toArray)
    }, new java.util.HashMap[String, Array[String]])
  else new java.util.HashMap[String, Array[String]]

  final lazy val localaddress = exchange.socketChannel.asInstanceOf[aio.SocketChannelWithTimeout].channel.getLocalAddress.asInstanceOf[java.net.InetSocketAddress]

  final lazy val remoteaddress = exchange.socketChannel.asInstanceOf[aio.SocketChannelWithTimeout].channel.getRemoteAddress.asInstanceOf[java.net.InetSocketAddress]

}

