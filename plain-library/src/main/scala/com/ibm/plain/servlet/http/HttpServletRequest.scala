package com.ibm

package plain

package servlet

package http

import java.io.{ BufferedReader, PrintWriter }
import java.util.{ Enumeration, Locale, Map ⇒ JMap }

import scala.collection.JavaConversions.{ asJavaEnumeration, mapAsJavaMap, mapAsScalaMap }

import javax.{ servlet ⇒ js }
import plain.http.Request

final case class HttpServletRequest(

  private final val request: Request,

  private final val servletcontext: ServletContext)

  extends js.http.HttpServletRequest

  with helper.HasAttributes {

  final def authenticate(x$1: javax.servlet.http.HttpServletResponse): Boolean = unsupported

  final def changeSessionId: String = unsupported

  final def getAuthType: String = unsupported

  final def getContextPath: String = unsupported

  final def getCookies: Array[javax.servlet.http.Cookie] = unsupported

  final def getDateHeader(x$1: String): Long = unsupported

  final def getIntHeader(x$1: String): Int = unsupported

  final def getMethod: String = request.method.toString

  final def getPart(x$1: String): javax.servlet.http.Part = unsupported

  final def getParts: java.util.Collection[javax.servlet.http.Part] = unsupported

  final def getPathInfo: String = null // unsupported

  final def getPathTranslated: String = unsupported

  final def getQueryString: String = request.query match { case Some(query) ⇒ query case _ ⇒ null }

  final def getRemoteUser: String = unsupported

  final def getRequestURI: String = "/Users/guido/Development/forks/FrameworkBenchmarks-1/plain/web-apps/servlet/WEB-INF/jsp/hello.jsp"

  final def getRequestURL: StringBuffer = unsupported

  final def getRequestedSessionId: String = unsupported

  final def getServletPath: String = { println(request.path.toString); request.path.mkString("/") }

  final def getSession: javax.servlet.http.HttpSession = unsupported

  final def getSession(x$1: Boolean): javax.servlet.http.HttpSession = unsupported

  final def getUserPrincipal: java.security.Principal = unsupported

  final def isRequestedSessionIdFromCookie: Boolean = unsupported

  final def isRequestedSessionIdFromURL: Boolean = unsupported

  final def isRequestedSessionIdFromUrl: Boolean = unsupported

  final def isRequestedSessionIdValid: Boolean = unsupported

  final def isUserInRole(x$1: String): Boolean = unsupported

  final def login(x$1: String, x$2: String) = unsupported

  final def logout = unsupported

  final def upgrade[T <: javax.servlet.http.HttpUpgradeHandler](x$1: Class[T]): T = unsupported

  final def getCharacterEncoding: String = unsupported

  final def setCharacterEncoding(arg0: String) = unsupported

  final def getContentLength: Int = unsupported

  final def getContentLengthLong: Long = { 0L }

  final def getContentType: String = unsupported

  final def getInputStream: js.ServletInputStream = unsupported

  final def getParameter(name: String): String = getParameterMap.get(name) match { case null ⇒ null case values ⇒ values.head }

  final def getParameterNames: Enumeration[String] = getParameterMap.keysIterator

  final def getParameterValues(name: String): Array[String] = getParameterMap.get(name)

  final lazy val getParameterMap: JMap[String, Array[String]] =
    ignoreOrElse(rest.Matching.default.decodeForm(request.entity) map { case (name, values) ⇒ (name, values.toArray) }, new java.util.HashMap[String, Array[String]])

  final def getProtocol: String = unsupported

  final def getScheme: String = unsupported

  final def getServerName: String = unsupported

  final def getServerPort: Int = unsupported

  final def getReader: BufferedReader = unsupported

  final def getRemoteAddr: String = unsupported

  final def getRemoteHost: String = unsupported

  final def getLocale: Locale = unsupported

  final def getLocales: Enumeration[Locale] = unsupported

  final def isSecure: Boolean = unsupported

  final def getRequestDispatcher(path: String): js.RequestDispatcher = RequestDispatcher(path)

  final def getRealPath(arg0: String): String = unsupported

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

  final def getHeader(arg0: String): String = unsupported

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

}

