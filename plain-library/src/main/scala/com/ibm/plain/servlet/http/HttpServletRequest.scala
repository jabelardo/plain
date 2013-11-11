package com.ibm

package plain

package servlet

package http

import java.io.{ BufferedReader, PrintWriter }
import java.util.{ Enumeration, Locale, Map ⇒ JMap }

import javax.servlet.{ AsyncContext, DispatcherType, RequestDispatcher, ServletContext, ServletInputStream, ServletOutputStream, ServletRequest, ServletResponse }
import javax.servlet.http.{ Cookie, HttpServletRequest ⇒ JHttpServletRequest }

import plain.http.Request

final case class HttpServletRequest(

  request: Request)

  extends JHttpServletRequest {

  def authenticate(x$1: javax.servlet.http.HttpServletResponse): Boolean = unsupported

  def changeSessionId: String = unsupported

  def getAuthType: String = unsupported

  def getContextPath: String = unsupported

  def getCookies: Array[javax.servlet.http.Cookie] = unsupported

  def getDateHeader(x$1: String): Long = unsupported

  def getIntHeader(x$1: String): Int = unsupported

  def getMethod: String = request.method.toString

  def getPart(x$1: String): javax.servlet.http.Part = unsupported

  def getParts: java.util.Collection[javax.servlet.http.Part] = unsupported

  def getPathInfo: String = unsupported

  def getPathTranslated: String = unsupported

  def getQueryString: String = unsupported

  def getRemoteUser: String = unsupported

  def getRequestURI: String = unsupported

  def getRequestURL: StringBuffer = unsupported

  def getRequestedSessionId: String = unsupported

  def getServletPath: String = unsupported

  def getSession: javax.servlet.http.HttpSession = unsupported

  def getSession(x$1: Boolean): javax.servlet.http.HttpSession = unsupported

  def getUserPrincipal: java.security.Principal = unsupported

  def isRequestedSessionIdFromCookie: Boolean = unsupported

  def isRequestedSessionIdFromURL: Boolean = unsupported

  def isRequestedSessionIdFromUrl: Boolean = unsupported

  def isRequestedSessionIdValid: Boolean = unsupported

  def isUserInRole(x$1: String): Boolean = unsupported

  def login(x$1: String, x$2: String) = unsupported

  def logout = unsupported

  def upgrade[T <: javax.servlet.http.HttpUpgradeHandler](x$1: Class[T]): T = unsupported

  def getAttribute(arg0: String): Object = { null }

  def getAttributeNames: Enumeration[String] = { null }

  def getCharacterEncoding: String = { null }

  def setCharacterEncoding(arg0: String) = {}

  def getContentLength: Int = { 0 }

  def getContentLengthLong: Long = { 0L }

  def getContentType: String = { null }

  def getInputStream: ServletInputStream = { null }

  def getParameter(arg0: String): String = { null }

  def getParameterNames: Enumeration[String] = { null }

  def getParameterValues(arg0: String): Array[String] = { null }

  def getParameterMap: JMap[String, Array[String]] = { null }

  def getProtocol: String = { null }

  def getScheme: String = { null }

  def getServerName: String = { null }

  def getServerPort: Int = { 0 }

  def getReader: BufferedReader = { null }

  def getRemoteAddr: String = { null }

  def getRemoteHost: String = { null }

  def setAttribute(arg0: String, arg1: Object) = {}

  def removeAttribute(arg0: String) = {}

  def getLocale: Locale = { null }

  def getLocales: Enumeration[Locale] = { null }

  def isSecure: Boolean = { false }

  def getRequestDispatcher(arg0: String): RequestDispatcher = { null }

  def getRealPath(arg0: String): String = { null }

  def getRemotePort: Int = { 0 }

  def getLocalName: String = { null }

  def getLocalAddr: String = { null }

  def getLocalPort: Int = { 0 }

  def getServletContext: ServletContext = { null }

  def startAsync: AsyncContext = { null }

  def startAsync(arg0: ServletRequest, arg1: ServletResponse): AsyncContext = { null }

  def isAsyncStarted: Boolean = { false }

  def isAsyncSupported: Boolean = { false }

  def getAsyncContext: AsyncContext = { null }

  def getDispatcherType: DispatcherType = { null }

  def addCookie(arg0: Cookie) = {}

  def containsHeader(arg0: String): Boolean = { false }

  def encodeURL(arg0: String): String = { null }

  def encodeRedirectURL(arg0: String): String = { null }

  def encodeUrl(arg0: String): String = { null }

  def encodeRedirectUrl(arg0: String): String = { null }

  def sendError(arg0: Int, arg1: String) = {}

  def sendError(arg0: Int) = {}

  def sendRedirect(arg0: String) = {}

  def setDateHeader(arg0: String, arg1: Long) = {}

  def addDateHeader(arg0: String, arg1: Long) = {}

  def setHeader(name: String, value: String) = {}

  def addHeader(arg0: String, arg1: String) = {}

  def setIntHeader(arg0: String, arg1: Int) = {}

  def addIntHeader(arg0: String, arg1: Int) = {}

  def setStatus(arg0: Int) = {}

  def setStatus(arg0: Int, arg1: String) = {}

  def getStatus: Int = { 0 }

  def getHeader(arg0: String): String = { null }

  def getHeaders(arg0: String): Enumeration[String] = { null }

  def getHeaderNames: Enumeration[String] = { null }

  def getOutputStream: ServletOutputStream = { null }

  def getWriter: PrintWriter = { null }

  def setContentLength(arg0: Int) = {}

  def setContentLengthLong(arg0: Long) = {}

  def setContentType(arg0: String) = {}

  def setBufferSize(arg0: Int) = {}

  def getBufferSize: Int = { 0 }

  def flushBuffer = {}

  def resetBuffer = {}

  def isCommitted: Boolean = { false }

  def reset = {}

  def setLocale(arg0: java.util.Locale) = {}

}

