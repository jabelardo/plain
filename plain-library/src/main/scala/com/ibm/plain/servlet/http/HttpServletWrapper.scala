package com.ibm

package plain

package servlet

package http

import javax.{ servlet ⇒ js }

import scala.xml.Node
import reflect.Injector

class HttpServletWrapper(

  protected[this] final val servletcontext: ServletContext,

  protected[this] final val servletxml: Node)

  extends js.http.HttpServlet

  with ServletConfig {

  final def getHttpServlet = httpservlet

  override final def destroy = httpservlet.destroy

  override final def getServletConfig: js.ServletConfig = this

  override final def getServletInfo: String = getServletName

  override final def init(servletconfig: js.ServletConfig) = httpservlet.init(servletconfig)

  override final def service(request: js.ServletRequest, response: js.ServletResponse) = httpservlet.service(request, response)

  override def toString = if (null != httpservlet) httpservlet.toString else "HttpServletWrapper(null)"

  protected[this] final val httpservlet: js.http.HttpServlet = Injector(Class.forName(
    (servletxml \ "servlet-class").text, true, servletcontext.getClassLoader).newInstance.asInstanceOf[js.http.HttpServlet])

}
