package com.ibm

package plain

package servlet

import java.util.Enumeration
import javax.servlet.{ Servlet ⇒ JServlet, ServletConfig ⇒ JServletConfig, ServletRequest ⇒ JServletRequest, ServletResponse ⇒ JServletResponse }

import scala.xml.Node
import scala.language.postfixOps
import scala.collection.JavaConversions._

trait ServletXXWrapper

  extends JServlet

  with ServletConfig {

  override def toString = servlet.toString

  abstract override final def destroy = servlet.destroy

  abstract override final def getServletConfig: JServletConfig = this

  abstract override final def getServletInfo: String = "Not yet implemented"

  abstract override final def init(servletconfig: JServletConfig) = servlet.init(servletconfig)

  abstract override final def service(request: JServletRequest, response: JServletResponse) = servlet.service(request, response)

  protected[this] val servletcontext: ServletContext

  protected[this] val servletxml: Node

  protected[this] final val servlet: JServlet = Class.forName((servletxml \ "servlet-class").text, true, servletcontext.getClassLoader).newInstance.asInstanceOf[JServlet]

}
