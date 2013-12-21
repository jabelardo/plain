package com.ibm

package plain

package servlet

package http

import java.io.{ BufferedReader, PrintWriter }
import java.util.{ Enumeration, Locale, Map ⇒ JMap }

import scala.collection.JavaConversions.{ asJavaEnumeration, mapAsJavaMap, mapAsScalaMap }
import scala.collection.mutable.HashMap

import com.ibm.plain.servlet.ServletContext

import javax.{ servlet ⇒ js }
import plain.http.Request

final class HttpServletRequest(

  private final val request: Request,

  private final val servletcontext: ServletContext)

  extends js.http.HttpServletRequest

  with helper.HasAttributes {

  final def authenticate(x$1: js.http.HttpServletResponse): Boolean = unsupported

  final def changeSessionId: String = unsupported

  final def getAuthType: String = unsupported

  final def getContextPath: String = "/" + request.path.take(2).mkString("/")

  final def getCookies: Array[js.http.Cookie] = unsupported

  final def getDateHeader(x$1: String): Long = unsupported

  final def getIntHeader(x$1: String): Int = unsupported

  final def getMethod: String = request.method.toString

  final def getPart(x$1: String): js.http.Part = unsupported

  final def getParts: java.util.Collection[js.http.Part] = unsupported

  final def getPathInfo: String = null

  final def getPathTranslated: String = unsupported

  final def getQueryString: String = request.query match { case Some(query) ⇒ query case _ ⇒ null }

  final def getRemoteUser: String = unsupported

  final def getRequestURI: String = "/" + request.path.mkString("/")

  final def getRequestURL: StringBuffer = unsupported

  final def getRequestedSessionId: String = unsupported

  final def getServletPath: String = "/" + request.path.drop(2).mkString("/")

  final def getSession: js.http.HttpSession = new HttpSession("1", servletcontext)

  final def getSession(create: Boolean): js.http.HttpSession = { println("getSession " + create); getSession }

  final def getUserPrincipal: java.security.Principal = unsupported

  final def isRequestedSessionIdFromCookie: Boolean = unsupported

  final def isRequestedSessionIdFromURL: Boolean = unsupported

  final def isRequestedSessionIdFromUrl: Boolean = unsupported

  final def isRequestedSessionIdValid: Boolean = unsupported

  final def isUserInRole(x$1: String): Boolean = unsupported

  final def login(x$1: String, x$2: String) = unsupported

  final def logout = unsupported

  final def upgrade[T <: js.http.HttpUpgradeHandler](x$1: Class[T]): T = unsupported

  final def getCharacterEncoding: String = unsupported

  final def setCharacterEncoding(arg0: String) = unsupported

  final def getContentLength: Int = unsupported

  final def getContentLengthLong: Long = unsupported

  final def getContentType: String = unsupported

  final def getInputStream: js.ServletInputStream = unsupported

  final def getParameter(name: String): String = getParameterMap.get(name) match { case null ⇒ null case values ⇒ values.head }

  final def getParameterNames: Enumeration[String] = getParameterMap.keysIterator

  final def getParameterValues(name: String): Array[String] = getParameterMap.get(name)

  final lazy val getParameterMap: JMap[String, Array[String]] = ignoreOrElse(
    rest.Matching.default.decodeForm(request.entity) map {
      case (name, values) ⇒ (name, values.toArray)
    }, new java.util.HashMap[String, Array[String]])

  final def getProtocol: String = unsupported

  final def getScheme: String = unsupported

  final def getServerName: String = "127.0.0.1"

  final def getServerPort: Int = 9080

  final def getReader: BufferedReader = unsupported

  final def getRemoteAddr: String = "1.2.3.4"

  final def getRemoteHost: String = unsupported

  final def getLocale: Locale = Locale.getDefault

  final def getLocales: Enumeration[Locale] = List(getLocale).toIterator

  final def isSecure: Boolean = false

  final def getRequestDispatcher(path: String): js.RequestDispatcher = new RequestDispatcher(path, servletcontext)

  final def getRealPath(path: String): String = servletcontext.getRealPath(path)

  final def getRemotePort: Int = unsupported

  final def getLocalName: String = unsupported

  final def getLocalAddr: String = unsupported

  final def getLocalPort: Int = unsupported

  final def getServletContext: ServletContext = servletcontext

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
    case _ ⇒ println("not found : " + name); null
  }

  final def getHeaders(arg0: String): Enumeration[String] = unsupported

  final def getHeaderNames: Enumeration[String] = unsupported

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

  final def log(msg: String) = servletcontext.log(msg)

  protected[this] final val attributes = new HashMap[String, Object]

}

