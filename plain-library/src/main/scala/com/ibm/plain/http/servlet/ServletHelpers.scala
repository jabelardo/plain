package com.ibm

package plain

package http

package servlet

import java.io.{ BufferedReader, PrintWriter, InputStream }
import java.net.URL
import java.security.Principal
import java.util.{ Enumeration, Locale, Set ⇒ JSet, Map ⇒ JMap }

import scala.collection.JavaConversions.enumerationAsScalaIterator

import javax.servlet.{ RequestDispatcher ⇒ JRequestDispatcher, Servlet ⇒ JServlet }
import javax.servlet.http.{ HttpSession ⇒ JHttpSession, Cookie }

/**
 *
 */
private[servlet] object ServletHelpers {

  trait Attributes {

    final def getAttribute(name: String): Object = unsupported

    final def getAttributeNames: Enumeration[String] = unsupported

    final def setAttribute(name: String, value: Object) = unsupported

    final def removeAttribute(name: String) = unsupported

  }

  trait InitParameters {

    final def getInitParameter(name: String): String = unsupported

    final def getInitParameterNames: Enumeration[String] = unsupported

  }

  trait Parameters {

    final def getParameter(name: String): String = unsupported

    final def getParameterNames: Enumeration[String] = unsupported

    final def getParameterValues(name: String): Array[String] = unsupported

    final def getParameterMap: JMap[_, _] = unsupported

  }

  trait Headers {

    final def getHeader(name: String): String = unsupported

    final def getHeaderNames: Enumeration[String] = unsupported

    final def getHeaders(name: String): Enumeration[String] = unsupported

    final def getIntHeader(name: String): Int = unsupported

    final def getDateHeader(name: String): Long = unsupported

    final def setHeader(name: String, value: String) = unsupported

    final def setIntHeader(name: String, value: Int) = unsupported

    final def setDateHeader(name: String, value: Long) = unsupported

    final def addHeader(name: String, value: String) = unsupported

    final def addIntHeader(name: String, value: Int) = unsupported

    final def addDateHeader(name: String, value: Long) = unsupported

    final def containsHeader(name: String): Boolean = unsupported

  }

  trait Values {

    self: Attributes ⇒

    final def getValue(name: String): Object = getAttribute(name)

    final def getValueNames: Array[String] = getAttributeNames.toArray

    final def putValue(name: String, value: Object) = setAttribute(name, value)

    final def removeValue(name: String) = removeAttribute(name)

  }

  trait Logging {

    final def log(e: Exception, msg: String) = deprecated

    final def log(msg: String) = unsupported

    final def log(msg: String, e: Throwable) = unsupported

  }

  trait Servlets {

    final def getServlet(name: String): JServlet = deprecated

    final def getServletNames: Enumeration[String] = deprecated

    final def getServlets: Enumeration[JServlet] = deprecated

    final def getServletContextName: String = unsupported

  }

  trait Resources {

    final def getResource(path: String): URL = unsupported

    final def getResourceAsStream(path: String): InputStream = unsupported

    final def getResourcePaths(path: String): JSet[String] = unsupported

    final def getRealPath(path: String): String = unsupported

  }

  trait MimeTypes {

    final def getMimeType(file: String): String = unsupported

  }

  trait Dispatchers {

    final def getRequestDispatcher(path: String): JRequestDispatcher = unsupported

    final def getNamedDispatcher(name: String): JRequestDispatcher = unsupported

  }

  trait Times {

    final def getCreationTime: Long = unsupported

    final def getLastAccessedTime: Long = unsupported

    final def getMaxInactiveInterval: Int = unsupported

    final def setMaxInactiveInterval(interval: Int) = unsupported

    final def isNew: Boolean = unsupported

  }

  trait Locales {

    final def getLocale: Locale = unsupported

    final def getLocales: Enumeration[Locale] = unsupported

    final def setLocale(locale: Locale) = unsupported

  }

  trait NetInfos {

    final def getProtocol: String = unsupported

    final def getScheme: String = unsupported

    final def isSecure: Boolean = unsupported

    final def getRealPath(path: String): String = deprecated

    final def getLocalName: String = unsupported

    final def getLocalPort: Int = unsupported

    final def getLocalAddr: String = unsupported

    final def getRemoteName: String = unsupported

    final def getRemotePort: Int = unsupported

    final def getRemoteAddr: String = unsupported

    final def getRemoteHost: String = unsupported

    final def getServerName: String = unsupported

    final def getServerPort: Int = unsupported

  }

  trait Contents {

    final def getContentLength: Int = unsupported

    final def getContentType: String = unsupported

    final def getInputStream: ServletInputStream = unsupported

    final def getReader: BufferedReader = unsupported

    final def getOutputStream: ServletOutputStream = unsupported

    final def getWriter: PrintWriter = unsupported

    final def getCharacterEncoding: String = unsupported

    final def setContentLength(length: Int) = unsupported

    final def setContentType(contenttype: String) = unsupported

    final def setCharacterEncoding(encoding: String) = unsupported

  }

  trait Sessions {

    final def getSession: JHttpSession = unsupported

    final def getSession(create: Boolean): JHttpSession = unsupported

    final def getRequestedSessionId: String = unsupported

    final def isRequestedSessionIdFromCookie: Boolean = unsupported

    final def isRequestedSessionIdValid: Boolean = unsupported

    final def isRequestedSessionIdFromURL: Boolean = unsupported

    final def isRequestedSessionIdFromUrl: Boolean = isRequestedSessionIdFromURL

    final def getRequestURI: String = unsupported

    final def getRequestURL: StringBuffer = unsupported

  }

  trait Users {

    final def getRemoteUser: String = unsupported

    final def getUserPrincipal: Principal = unsupported

    final def isUserInRole(role: String): Boolean = unsupported

    final def getAuthType: String = unsupported

  }

  trait Paths {

    final def getMethod: String = unsupported

    final def getPathInfo: String = unsupported

    final def getPathTranslated: String = unsupported

    final def getServletPath: String = unsupported

    final def getQueryString: String = unsupported

    final def getContextPath: String = unsupported

  }

  trait Cookies {

    final def getCookies: Array[Cookie] = unsupported

    final def addCookie(cookie: Cookie) = unsupported

  }

  trait Buffers {

    final def flushBuffer = unsupported

    final def reset = unsupported

    final def resetBuffer = unsupported

    final def getBufferSize: Int = unsupported

    final def setBufferSize(size: Int) = unsupported

  }

  trait Statuses {

    final def isCommitted: Boolean = unsupported

    final def setStatus(status: Int) = unsupported

    final def setStatus(status: Int, msg: String) = deprecated

  }

  trait Encodings {

    final def encodeURL(url: String) = unsupported

    final def encodeUrl(url: String) = encodeURL(url)

    final def encodeRedirectURL(url: String) = unsupported

    final def encodeRedirectUrl(url: String) = encodeRedirectURL(url)

  }

  trait Sendings {

    final def sendError(status: Int) = unsupported

    final def sendError(status: Int, msg: String) = unsupported

    final def sendRedirect(location: String) = unsupported

  }

}
