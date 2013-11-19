package com.ibm

package plain

package servlet

import java.io.{ File, InputStream }
import java.net.{ URL, URLClassLoader }
import java.util.{ Enumeration, EventListener, Map ⇒ JMap, Set ⇒ JSet }

import org.apache.jasper.Constants
import org.apache.jasper.servlet.JspServlet

import scala.collection.JavaConversions.{ asJavaEnumeration, enumerationAsScalaIterator, mapAsJavaMap, seqAsJavaList }
import scala.collection.concurrent.TrieMap
import scala.language.postfixOps
import scala.util.matching.Regex
import scala.xml.XML

import http.HttpServletWrapper
import javax.{ servlet ⇒ js }
import logging.HasLogger
import plain.io.{ classPathFromClassLoader, temporaryDirectory }

final class ServletContext(

  private[this] final val classloader: URLClassLoader)

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

  final def getJspConfigDescriptor: js.descriptor.JspConfigDescriptor = null

  final def getMajorVersion: Int = 3

  final def getMimeType(file: String): String = unsupported

  final def getMinorVersion: Int = 1

  final def getNamedDispatcher(name: String): js.RequestDispatcher = unsupported

  final def getRealPath(path: String): String = getResource(path) match {
    case null ⇒ null
    case url ⇒ ignoreOrElse(new File(url.toURI).getAbsolutePath, null)
  }

  final def getRequestDispatcher(path: String): js.RequestDispatcher = unsupported

  final def getResource(path: String): URL = classloader.getResource(if (path.startsWith("/")) path.drop(1) else path)

  final def getResourceAsStream(path: String): InputStream = classloader.getResourceAsStream(if (path.startsWith("/")) path.drop(1) else path)

  final def getResourcePaths(path: String): JSet[String] = new java.util.HashSet[String](classloader.getResources(path).map(_.toString).toList)

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

  private[servlet] final def getJspServlet = jspservlet

  private[servlet] final val webxml = XML.load(classloader.getResourceAsStream("WEB-INF/web.xml"))

  private[this] final val init: Unit = {
    setAttribute("com.sun.faces.useMyFaces", Boolean.box(false))
    setAttribute("org.glassfish.jsp.isStandaloneWebapp", Boolean.box(true))
    setAttribute(js.ServletContext.TEMPDIR, temporaryDirectory)
    setAttribute(Constants.SERVLET_CLASSPATH, classPathFromClassLoader(classloader))
    setAttribute(Constants.JSP_RESOURCE_INJECTOR_CONTEXT_ATTRIBUTE, new org.glassfish.jsp.api.ResourceInjector {

      final def createTagHandlerInstance[T <: javax.servlet.jsp.tagext.JspTag](tagclass: Class[T]): T = tagclass.newInstance

      final def preDestroy(tag: javax.servlet.jsp.tagext.JspTag) = ()

      private[this] final val cache = new TrieMap[String, javax.servlet.jsp.tagext.JspTag]

    })
  }

  private[this] final val welcomefiles = (webxml \ "welcome-file-list" \ "welcome-file") map (_.text)

  private[this] final val effectiveversion = try {
    (webxml \ "@version").text.split('.').toList match {
      case List("") ⇒ List(3, 1)
      case l ⇒ l.map(_.toInt)
    }
  } catch { case _: Throwable ⇒ List(3, 1) }

  private[this] final val applicationname = classloader.toString

  private[this] final val contextparameters = {
    val m = new TrieMap[String, String]
    m ++= ((webxml \ "context-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap)
    m
  }

  private[this] final val servlets = (webxml \ "servlet").map { servletxml ⇒
    val loadonstartup = if ((servletxml \ "load-on-startup").isEmpty) 0 else (servletxml \ "load-on-startup").text match { case "" ⇒ 0 case i ⇒ i.toInt }
    if (0 < loadonstartup) warning("load-on-startup " + loadonstartup + " ignored during bootstrapping.")
    val servlet = new HttpServletWrapper(this, servletxml)
    servlet.init(servlet)
    (servlet.getServletName, servlet)
  }.toMap

  private[this] final val servletmappings: Map[Regex, js.Servlet] = {
    def mappings(attribute: Boolean) = (webxml \ "servlet-mapping").map { mapping ⇒
      def pattern(p: String) = (mapping \ ((if (attribute) "@" else "") + p)) match { case u if u.isEmpty ⇒ None case u ⇒ Some(u.text.r) }
      (pattern("url-pattern").getOrElse(pattern("url-regexp").getOrElse(null)), servlets.getOrElse((mapping \ ((if (attribute) "@" else "") + "servlet-name")).text, null))
    }.filter(_._1 != null).toMap

    mappings(false) ++ mappings(true)
  }

  private[this] final val jspservlet: js.Servlet = {
    val config =
      <servlet>
        <servlet-name>JSP</servlet-name>
        <init-param>
          <param-name>fork</param-name>
          <param-value>false</param-value>
        </init-param>
        <init-param>
          <param-name>xpoweredBy</param-name>
          <param-value>false</param-value>
        </init-param>
        <init-param>
          <param-name>enablePooling</param-name>
          <param-value>true</param-value>
        </init-param>
        <init-param>
          <param-name>javaEncoding</param-name>
          <param-value>UTF8</param-value>
        </init-param>
      </servlet>
    val jsp = new JspServlet
    jsp.init(new ManualServletConfig(config, this))
    jsp
  }

}

