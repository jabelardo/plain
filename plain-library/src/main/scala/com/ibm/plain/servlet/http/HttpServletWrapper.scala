package com.ibm

package plain

package servlet

package http

import javax.servlet.{ ServletConfig ⇒ JServletConfig, ServletRequest ⇒ JServletRequest, ServletResponse ⇒ JServletResponse }
import javax.servlet.http.{ HttpServlet ⇒ JHttpServlet, HttpServletRequest ⇒ JHttpServletRequest, HttpServletResponse ⇒ JHttpServletResponse }

import scala.xml.Node
import reflect.Injector

class HttpServletWrapper(

  protected[this] final val servletcontext: ServletContext,

  protected[this] final val servletxml: Node)

  extends JHttpServlet

  with ServletConfig {

  override def toString = httpservlet.toString

  override final def destroy = httpservlet.destroy

  override final def getServletConfig: JServletConfig = this

  override final def getServletInfo: String = getServletName

  override final def init(servletconfig: JServletConfig) = httpservlet.init(servletconfig)

  override final def service(request: JServletRequest, response: JServletResponse) = httpservlet.service(request, response)

  protected[this] final val httpservlet: JHttpServlet = Injector(
    Class.forName((servletxml \ "servlet-class").text, true, servletcontext.getClassLoader).newInstance.asInstanceOf[JHttpServlet])

}
