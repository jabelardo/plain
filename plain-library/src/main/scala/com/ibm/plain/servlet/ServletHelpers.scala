package com.ibm

package plain

package servlet

import java.io.{ BufferedReader, InputStream, PrintWriter }
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.net.URL
import java.security.Principal
import java.util.{ Collections, Enumeration, Locale, Map ⇒ JMap, Set ⇒ JSet }

import scala.collection.JavaConversions.{ asJavaCollection, enumerationAsScalaIterator, mutableMapAsJavaMap, seqAsJavaList }
import scala.collection.mutable.OpenHashMap

import javax.servlet.{ RequestDispatcher ⇒ JRequestDispatcher, Servlet ⇒ JServlet }
import javax.servlet.http.{ Cookie, HttpSession ⇒ JHttpSession }
import rest.Context

trait Contexts {

  protected[this] val context: Context

}

trait Attributes {

  final def getAttribute(name: String): Object = attributes.get(name) match { case Some(value) ⇒ value case _ ⇒ println(this + " no attr : " + name); null }

  final def getAttributeNames: Enumeration[String] = Collections.enumeration[String](attributes.keys)

  final def setAttribute(name: String, value: Object): Unit = { println(this + " set attr " + name + "=" + value); attributes.put(name, value) }

  final def removeAttribute(name: String): Unit = attributes.remove(name)

  private[this] final val attributes = new OpenHashMap[String, Object]

}

trait Values {

  self: Attributes ⇒

  final def getValue(name: String): Object = getAttribute(name)

  final def getValueNames: Array[String] = getAttributeNames.toArray

  final def putValue(name: String, value: Object) = setAttribute(name, value)

  final def removeValue(name: String) = removeAttribute(name)

}

trait InitParameters {

  final def getInitParameter(name: String): String = initparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ println("no initparam : " + name); null }

  final def getInitParameterNames: Enumeration[String] = Collections.enumeration[String](initparameters.keys)

  final def setInitParameter(name: String, value: String): Unit = initparameters.put(name, value)

  private[this] final val initparameters = new OpenHashMap[String, String]

}

trait Parameters {

  final def getParameter(name: String): String = parameters.get(name) match { case Some(value) ⇒ value case _ ⇒ println("no param : " + name); null }

  final def getParameterNames: Enumeration[String] = Collections.enumeration[String](parameters.keys)

  final def getParameterValues(name: String): Array[String] = parameters.get(name) match { case Some(value) ⇒ Array(value) case _ ⇒ null }

  final def getParameterMap: JMap[String, String] = new java.util.HashMap[String, String](parameters)

  final def setParameter(name: String, value: String): Unit = parameters.put(name, value)

  private[this] final val parameters = new OpenHashMap[String, String]

}

trait Headers {

  self: Contexts ⇒

  final def getHeader(name: String): String = context.request.headers.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  final def getHeaderNames: Enumeration[String] = Collections.enumeration[String](context.request.headers.keys)

  final def getHeaders(name: String): Enumeration[String] = context.request.headers.get(name) match { case Some(value) ⇒ Collections.enumeration[String](List(value)) case _ ⇒ null }

  final def getIntHeader(name: String): Int = unsupported

  final def getDateHeader(name: String): Long = unsupported

  final def setHeader(name: String, value: String): Unit = context.response.headers = context.response.headers ++ Map(name -> value)

  final def setIntHeader(name: String, value: Int): Unit = unsupported

  final def setDateHeader(name: String, value: Long): Unit = context.response.headers = context.response.headers ++ Map(name -> new java.util.Date(value).toGMTString)

  final def addHeader(name: String, value: String): Unit = unsupported

  final def addIntHeader(name: String, value: Int): Unit = unsupported

  final def addDateHeader(name: String, value: Long): Unit = unsupported

  final def containsHeader(name: String): Boolean = context.request.headers.contains(name)

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

  final def getLocale: Locale = locale

  final def getLocales: Enumeration[Locale] = Collections.enumeration(Locale.getAvailableLocales.toList)

  final def setLocale(locale: Locale) = this.locale = locale

  private[this] final var locale: Locale = Locale.getDefault

}

trait NetInfos {

  self: Contexts ⇒

  final def getProtocol: String = http.Version.`HTTP/1.1`.toString

  final def getScheme: String = "http"

  final def isSecure: Boolean = false

  final def getRealPath(path: String): String = deprecated

  final def getLocalName: String = "localaddr"

  final def getLocalPort: Int = 0

  final def getLocalAddr: String = "localaddr"

  final def getRemoteName: String = "remotename"

  final def getRemotePort: Int = unsupported

  final def getRemoteAddr: String = "remoteaddr"

  final def getRemoteHost: String = "remotehost"

  final def getServerName: String = "servername"

  final def getServerPort: Int = 0

}

trait Contents {

  self: Contexts ⇒

  final def getContentLength: Int = unsupported

  final lazy val getContentType: String = { bytebuffer.flip; new String(bytebuffer.array, 0, bytebuffer.remaining) }

  final def getInputStream: ServletInputStream = unsupported

  final def getReader: BufferedReader = unsupported

  final def getOutputStream: ServletOutputStream = ServletOutputStream(new io.ByteBufferOutputStream(bytebuffer))

  final def getWriter: PrintWriter = unsupported

  final def getCharacterEncoding: String = unsupported

  final def setContentLength(length: Int) = { println("contentlength " + length) }

  final def setContentType(contenttype: String) = { println("contenttype " + contenttype) }

  final def setCharacterEncoding(encoding: String) = unsupported

  private[this] final val bytebuffer: ByteBuffer = ByteBuffer.allocate(10000)

}

trait Sessions {

  final def getSession: JHttpSession = HttpSession.apply

  final def getSession(create: Boolean): JHttpSession = HttpSession.apply

  final def getRequestedSessionId: String = unsupported

  final def isRequestedSessionIdFromCookie: Boolean = unsupported

  final def isRequestedSessionIdValid: Boolean = unsupported

  final def isRequestedSessionIdFromURL: Boolean = unsupported

  final def isRequestedSessionIdFromUrl: Boolean = isRequestedSessionIdFromURL

}

trait Users {

  final def getRemoteUser: String = unsupported

  final def getUserPrincipal: Principal = unsupported

  final def isUserInRole(role: String): Boolean = unsupported

  final def getAuthType: String = unsupported

}

trait Paths {

  self: Contexts ⇒

  final def getMethod: String = unsupported

  final def getPathInfo: String = if (null != context.remainder) context.remainder.mkString("/", "/", "") else null

  final def getPathTranslated: String = unsupported

  final def getServletPath: String = getRequestURI

  final def getQueryString: String = context.request.query match { case Some(value) ⇒ value case _ ⇒ null }

  final def getContextPath: String = ""

  final def getRequestURI: String = context.request.path.mkString("/", "/", "")

  final def getRequestURL: StringBuffer = new StringBuffer(getRequestURI)

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

