package com.ibm

package plain

package servlet

import java.io.{ File, InputStream }
import java.net.{ URL, URLClassLoader }
import java.util.{ Enumeration, EventListener, Map ⇒ JMap, Set ⇒ JSet }

import org.apache.commons.io.{ FileUtils, FilenameUtils }

import scala.collection.JavaConversions.{ asJavaEnumeration, enumerationAsScalaIterator, mapAsJavaMap, seqAsJavaList, collectionAsScalaIterable }

import scala.collection.concurrent.TrieMap
import scala.language.postfixOps
import scala.util.matching.Regex
import scala.xml.XML

import concurrent.scheduleOnce
import reflect.Injector
import javax.{ servlet ⇒ js }
import plain.io.{ classPathFromClassLoader, temporaryDirectory, ByteArrayOutputStream, PrintWriter }
import plain.http.{ MimeType, Request, Response }
import http.{ HttpServletRequest, HttpServletResponse }

final class ServletContext(

  private[this] final val classloader: URLClassLoader,

  private[this] final val root: String)

  extends js.ServletContext

  with HasAttributes

  with logging.Logger {

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
    ignore(classloader.close)
    filters.values.map(_._1).foreach { f ⇒ try f.destroy catch { case e: Throwable ⇒ trace(e.toString) } }
    servlets.values.map(_._1).map(s ⇒ if (s.isLeft) s.left.get._2 else s.right.get).foreach { s ⇒ try s.destroy catch { case e: Throwable ⇒ trace(e.toString) } }
    listeners.foreach { l ⇒ try l.contextDestroyed(new js.ServletContextEvent(this)) catch { case e: Throwable ⇒ trace(e.toString) } }
    jsppages.cancel(true)
  }

  final def getClassLoader: ClassLoader = classloader

  final def getContext(uripath: String): ServletContext = unsupported

  final val getContextPath: String = "/" + FilenameUtils.getBaseName(root)

  final def getDefaultSessionTrackingModes: JSet[js.SessionTrackingMode] = unsupported

  final def getEffectiveMajorVersion: Int = effectiveversion(0)

  final def getEffectiveMinorVersion: Int = effectiveversion(1)

  final def getEffectiveSessionTrackingModes: JSet[js.SessionTrackingMode] = unsupported

  final def getFilterRegistration(filterName: String): js.FilterRegistration = unsupported

  final def getFilterRegistrations: JMap[String, _ <: js.FilterRegistration] = unsupported

  final def getInitParameter(name: String): String = contextparameters.get(name) match { case Some(value) ⇒ value case _ ⇒ null }

  final def getInitParameterNames: Enumeration[String] = contextparameters.keysIterator

  final def getJspConfigDescriptor: js.descriptor.JspConfigDescriptor = null

  final def getMajorVersion: Int = version(0)

  final def getMimeType(file: String): String = MimeType.forExtension(FilenameUtils.getExtension(file)) match {
    case Some(mimetype) ⇒ mimetype.name
    case _              ⇒ null
  }

  final def getMinorVersion: Int = version(1)

  final def getNamedDispatcher(name: String): js.RequestDispatcher = unsupported

  final def getRealPath(path: String): String = getResource(path) match {
    case null ⇒ null
    case url  ⇒ ignoreOrElse(new File(url.toURI).getAbsolutePath, null)
  }

  final val getRealPath = FilenameUtils.normalize(FilenameUtils.removeExtension(root))

  final def getRequestDispatcher(path: String): js.RequestDispatcher = unsupported

  final def getResource(path: String): URL = classloader.getResource(if (path.startsWith("/")) path.drop(1) else path)

  final def getResourceAsStream(path: String): InputStream = classloader.getResourceAsStream(if (path.startsWith("/")) path.drop(1) else path)

  final def getResourcePaths(path: String): JSet[String] = new java.util.HashSet[String](classloader.getResources(path).map(_.toString).toList)

  final def getServerInfo: String = unsupported

  final def getServlet(name: String): js.Servlet = servlets.get(name) match {
    case Some((Left((_, servlet)), _, _)) ⇒ servlet
    case Some((Right(servlet), _, _))     ⇒ servlet
    case _                                ⇒ null
  }

  final def getServletContextName: String = (webxml \ "display-name").text.trim match { case "" ⇒ getContextPath.replace("/", "") case s ⇒ s }

  final def getServletNames: Enumeration[String] = servlets.keysIterator

  final def getServletRegistration(name: String): js.ServletRegistration = unsupported

  final def getServletRegistrations: JMap[String, _ <: js.ServletRegistration] = unsupported

  final def getServlets: Enumeration[js.Servlet] = servlets.values.map(_._1).map(s ⇒ if (s.isLeft) s.left.get._2 else s.right.get).toIterator

  final def getSessionCookieConfig: js.SessionCookieConfig = unsupported

  final def getVirtualServerName: String = unsupported

  final def log(e: Exception, msg: String) = log(msg, e)

  final def log(msg: String) = info(msg)

  final def log(msg: String, e: Throwable) = log(msg + " : " + e)

  final def setInitParameter(name: String, value: String): Boolean = contextparameters.put(name, value) match { case None ⇒ false case _ ⇒ true }

  final def setSessionTrackingModes(modes: JSet[js.SessionTrackingMode]) = unsupported

  final def getServletMappings = servletmappings

  final def getHttpServlets = servlets.values

  private[servlet] final def getJspServlet = jspservlet

  private[this] final val webxml = XML.load(classloader.getResourceAsStream("WEB-INF/web.xml"))

  private[this] final val version = List(3, 1)

  private[this] final val welcomefiles = (webxml \ "welcome-file-list" \ "welcome-file") map (_.text.trim)

  private[this] final val effectiveversion = try {
    (webxml \ "@version").text.split('.').toList match {
      case List("") ⇒ version
      case l        ⇒ l.map(_.toInt)
    }
  } catch { case _: Throwable ⇒ version }

  private[this] final val contextparameters = {
    val m = new TrieMap[String, String]
    m ++= ((webxml \ "context-param") map { p ⇒ (p \ "param-name" text, p \ "param-value" text) } toMap)
    m
  }

  private[this] final val listeners: Seq[js.ServletContextListener] = (webxml \ "listener").map { listener ⇒
    Class.forName((listener \ "listener-class").text.trim, true, classloader).newInstance.asInstanceOf[js.ServletContextListener]
  }

  private[this] final val servlets: Map[String, (Either[(Int, js.http.HttpServlet), js.http.HttpServlet], js.ServletConfig, ServletContext)] = {
    setAttribute(org.apache.jasper.Constants.SERVLET_CLASSPATH, classPathFromClassLoader(classloader))
    setAttribute(org.apache.jasper.Constants.JSP_RESOURCE_INJECTOR_CONTEXT_ATTRIBUTE, new org.glassfish.jsp.api.ResourceInjector {
      final def createTagHandlerInstance[T <: js.jsp.tagext.JspTag](tagclass: Class[T]): T = tagclass.newInstance
      final def preDestroy(tag: js.jsp.tagext.JspTag) = ()
    })
    setAttribute("com.sun.faces.useMyFaces", Boolean.box(false))
    setAttribute("org.glassfish.jsp.isStandaloneWebapp", Boolean.box(false))
    setAttribute(js.ServletContext.TEMPDIR, temporaryDirectory)
    listeners.foreach { listener ⇒
      debug("Initializing listener: " + listener.getClass.getName + " " + listener.getClass.getClassLoader)
      try listener.contextInitialized(new js.ServletContextEvent(this)) catch { case e: Throwable ⇒ error("Listener not initialized : " + e) }
    }
    (webxml \ "servlet").map { servletxml ⇒
      val loadonstartup = if ((servletxml \ "load-on-startup").isEmpty) 0 else (servletxml \ "load-on-startup").text match { case "" ⇒ 0 case i ⇒ i.toInt }
      val servlet = try
        Injector(Class.forName(
          (servletxml \ "servlet-class").text.trim, true, getClassLoader).newInstance.asInstanceOf[js.http.HttpServlet])
      catch { case e: Throwable ⇒ error("Servlet not initialized : " + e); null }
      val servletconfig = new WebXmlServletConfig(servletxml, this)
      (servletconfig.getServletName, (if (0 < loadonstartup) Left((loadonstartup, servlet)) else Right(servlet), servletconfig, this))
    }.filter(_._2 != null).toMap
  }

  private[this] final val loadedservlets = servlets.toSeq.
    filter { case (_, value) ⇒ value._1.isLeft }.
    map { case (_, value) ⇒ (value._1.left.get, value._2) }.
    sortWith { case (a, b) ⇒ a._1._1 < b._1._1 }.
    map { case ((_, servlet), servletconfig) ⇒ servlet.init(servletconfig); servlet }

  private[this] final val servletmappings: Map[String, String] = {
    def handleRegex(regex: Regex) = regex.toString match {
      case r if r.startsWith("^") && r.endsWith("$") ⇒ r.drop(1).dropRight(1)
      case r                                         ⇒ r
    }
    def mappings(attribute: Boolean) = (webxml \ "servlet-mapping").map { mapping ⇒
      def pattern(p: String) = (mapping \ ((if (attribute) "@" else "") + p)) match { case u if u.isEmpty ⇒ None case u ⇒ Some(u.text.r) }
      (pattern("url-pattern").getOrElse(pattern("url-regexp").getOrElse(null)), servlets.getOrElse((mapping \ ((if (attribute) "@" else "") + "servlet-name")).text, null))
    }.filter(_._1 != null).toMap

    (mappings(false) ++ mappings(true)).filter(_._2 != null).map { case (regex, (servlet, servletconfig, servletcontext)) ⇒ (servletconfig.getServletName, handleRegex(regex)) }
  }

  private[this] final val jspservlet: js.http.HttpServlet = {
    val systemUris = classOf[org.apache.jasper.runtime.TldScanner].getDeclaredField("systemUris")
    systemUris.setAccessible(true)
    systemUris.get(null).asInstanceOf[java.util.Set[_]].clear
    systemUris.setAccessible(false)
    Class.forName("org.apache.jasper.compiler.JspRuntimeContext", true, classloader)
    val jsp = new org.apache.jasper.servlet.JspServlet
    val config =
      <servlet>
        <servlet-name>JSP</servlet-name>
        <init-param>
          <param-name>fork</param-name>
          <param-value>true</param-value>
        </init-param>
        <init-param>
          <param-name>compilerSourceVM</param-name>
          <param-value>1.7</param-value>
        </init-param>
        <init-param>
          <param-name>compilerTargetVM</param-name>
          <param-value>1.7</param-value>
        </init-param>
        <init-param>
          <param-name>xpoweredBy</param-name>
          <param-value>false</param-value>
        </init-param>
      </servlet>
    jsp.init(new WebXmlServletConfig(config, this))
    jsp
  }

  private[this] final val jsppages = scheduleOnce(precompileJspPagesStartDelay) {
    if (precompileJspPages) {
      val jspfiles = FileUtils.listFiles(new File(getRealPath), Array("jsp"), true)
      if (0 < jspfiles.size) {
        trace(getRealPath + " : " + jspfiles.size + " jsp file(s) found.")
        jspfiles.foreach { jsp ⇒
          val path = jsp.getAbsolutePath.replace(getRealPath, "")
          try {
            val printwriter = new PrintWriter(new ByteArrayOutputStream(io.defaultBufferSize))
            val request = new HttpServletRequest(Request.Get(path), null, this, jspservlet)
            val response = new HttpServletResponse(Response(null, null), this, printwriter, jspservlet)
            new RequestDispatcher(path, this).forward(request, response)
          } catch {
            case _: Throwable ⇒
          } finally trace("jsp file precompiled : " + jsp)
        }
        info(getRealPath + " : " + jspfiles.size + " jsp file(s) precompiled.")
      }
    }
  }

  private[this] final val filters = {
    val m = (webxml \ "filter").map { filterxml ⇒
      val filter = Class.forName(
        (filterxml \ "filter-class").text.trim, true, getClassLoader).newInstance.asInstanceOf[js.Filter]
      val filterconfig = new WebXmlFilterConfig(filterxml, this)
      filter.init(filterconfig)
      (filterconfig.getFilterName, (filter, filterconfig))
    }.toMap
    m
  }

}

