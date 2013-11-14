package com.ibm

package plain

package servlet

import java.io.InputStream
import java.net.URL
import java.util.{ Enumeration, EventListener, Map ⇒ JMap, Set ⇒ JSet }

import scala.collection.JavaConversions.{ asJavaEnumeration, mapAsJavaMap }
import scala.collection.concurrent.TrieMap
import scala.language.postfixOps
import scala.util.matching.Regex
import scala.xml.XML

import http.HttpServletWrapper
import javax.{ servlet ⇒ js }
import logging.HasLogger

final class ServletContext(

  private[this] final val classloader: ClassLoader)

  extends js.ServletContext

  with helper.HasAttributes

  with HasLogger {

  final def addFilter(filterName: String, filterClass: Class[_ <: js.Filter]) = unsupported

  final def addFilter(filterName: String, filter: js.Filter) = unsupported

  final def addFilter(filterName: String, className: String) = unsupported

  final def addListener(listenerClass: Class[_ <: EventListener]) = unsupported

  final def addListener(className: String) = unsupported

  final def addListener[E <: EventListener](listener: E) = unsupported

  final def addServlet(name: String, servletClass: Class[_ <: js.Servlet]): js.ServletRegistration.Dynamic = unsupported

  final def addServlet(name: String, servlet: js.Servlet): js.ServletRegistration.Dynamic = unsupported

  final def addServlet(name: String, className: String): js.ServletRegistration.Dynamic = unsupported

  final def createFilter[E <: js.Filter](filterClass: Class[E]): E = unsupported

  final def createListener[E <: EventListener](listenerClass: Class[E]): E = unsupported

  final def createServlet[E <: js.Servlet](servletClass: Class[E]): E = unsupported

  final def declareRoles(roles: String*) = unsupported

  final def destroy = {
    servlets.values.foreach(servlet ⇒ ignore(servlet.destroy))
    servlets.clear
  }

  final def getApplicationName = applicationname

  final def getClassLoader: ClassLoader = classloader

  final def getContext(uripath: String): ServletContext = unsupported

  final def getContextPath: String = unsupported

  final def getDefaultSessionTrackingModes: JSet[js.SessionTrackingMode] = unsupported

  final def getEffectiveMajorVersion: Int = effectiveversion(0)

  final def getEffectiveMinorVersion: Int = effectiveversion(1)

  final def getEffectiveSessionTrackingModes: JSet[js.SessionTrackingMode] = unsupported

  final def getFilterRegistration(filterName: String): js.FilterRegistration = unsupported

  final def getFilterRegistrations: JMap[String, _ <: js.FilterRegistration] = unsupported

  final def getInitParameter(name: String): String = contextparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  final def getInitParameterNames: Enumeration[String] = contextparameters.keysIterator

  final def getJspConfigDescriptor: js.descriptor.JspConfigDescriptor = { info("getJspConfigDescriptor"); null }

  final def getMajorVersion: Int = 3

  final def getMimeType(file: String): String = unsupported

  final def getMinorVersion: Int = 1

  final def getNamedDispatcher(name: String): js.RequestDispatcher = unsupported

  final def getRealPath(path: String): String = { info("getRealPath " + path); "WEB-INF/jsp/fortunes.jsp" }

  final def getRequestDispatcher(path: String): js.RequestDispatcher = unsupported

  final def getResource(path: String): URL = {
    info("getResource " + path);
    val url = classloader.getResource("META-INF/fortunes.jsp")
    info(url.toString)
    val conn = url.openConnection
    info(conn.getContentLength.toString)
    url
  }

  final def getResourceAsStream(path: String): InputStream = unsupported

  final def getResourcePaths(path: String): JSet[String] = unsupported

  final def getServerInfo: String = unsupported

  final def getServlet(name: String): js.Servlet = servlets.getOrElse(name, null)

  final def getServletContextName: String = (webxml \ "display-name").text match { case "" ⇒ applicationname case s ⇒ s }

  final def getServletNames: Enumeration[String] = servlets.keysIterator

  final def getServletRegistration(name: String): js.ServletRegistration = unsupported

  final def getServletRegistrations: JMap[String, _ <: js.ServletRegistration] = unsupported

  final def getServlets: Enumeration[js.Servlet] = servlets.values.toIterator

  final def getSessionCookieConfig: js.SessionCookieConfig = unsupported

  final def getVirtualServerName: String = unsupported

  final def log(e: Exception, msg: String) = log(msg, e)

  final def log(msg: String) = info(msg)

  final def log(msg: String, e: Throwable) = log(msg + " : " + e.getMessage)

  final def setInitParameter(name: String, value: String): Boolean = contextparameters.put(name, value) match { case None ⇒ false case _ ⇒ true }

  final def setSessionTrackingModes(modes: JSet[js.SessionTrackingMode]) = unsupported

  private[servlet] final val webxml = XML.load(classloader.getResourceAsStream("WEB-INF/web.xml"))

  private[this] final val contextparameters = {
    val m = new TrieMap[String, String]
    m ++= ((webxml \ "context-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap)
    m
  }

  private[this] final val servlets = (webxml \ "servlet").map { servletxml ⇒
    val loadonstartup = if ((servletxml \ "load-on-startup").isEmpty) 0 else (servletxml \ "load-on-startup").text match { case "" ⇒ 0 case i ⇒ i.toInt }
    if (0 < loadonstartup) warning("load-on-startup " + loadonstartup + " ignored during bootstrapping.")
    val servlet = new HttpServletWrapper(this, servletxml)
    if (0 <= loadonstartup) servlet.init(servlet)
    (servlet.getServletName, servlet)
  }.toMap

  private[this] final val servletmappings: Map[Regex, js.Servlet] = {
    def mappings(attribute: Boolean) = (webxml \ "servlet-mapping").map { mapping ⇒
      def pattern(p: String) = (mapping \ ((if (attribute) "@" else "") + p)) match { case u if u.isEmpty ⇒ None case u ⇒ Some(u.text.r) }
      (pattern("url-pattern").getOrElse(pattern("url-regexp").getOrElse(null)), servlets.getOrElse((mapping \ ((if (attribute) "@" else "") + "servlet-name")).text, null))
    }.filter(_._1 != null).toMap

    mappings(false) ++ mappings(true)
  }

  private[this] final val welcomefiles = (webxml \ "welcome-file-list" \ "welcome-file") map (_.text)

  private[this] final val effectiveversion = (webxml \ "@version").text.split('.').toList match {
    case List("") ⇒ List(3, 1)
    case l ⇒ l.map(_.toInt)
  }

  private[this] final val applicationname = classloader.toString

}

