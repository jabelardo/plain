package com.ibm

package plain

package servlet

import java.util.Enumeration
import javax.servlet.{ Servlet ⇒ JServlet, ServletConfig ⇒ JServletConfig, ServletRequest ⇒ JServletRequest, ServletResponse ⇒ JServletResponse }

import scala.xml.Node
import scala.language.postfixOps
import scala.collection.JavaConversions._

class ServletWrapper(

  protected[this] final val servletcontext: ServletContext,

  protected[this] final val servletxml: Node)

  extends JServlet

  with ServletConfig {

  override def toString = servlet.toString

  final def destroy = servlet.destroy

  final def getServletConfig: JServletConfig = this

  def getServletInfo: String = "Not yet implemented"

  final def init(servletconfig: JServletConfig) = servlet.init(servletconfig)

  def service(request: JServletRequest, response: JServletResponse) = servlet.service(request, response)

  private[this] final val servlet: JServlet = Class.forName((servletxml \ "servlet-class").text, true, servletcontext.getClassLoader).newInstance.asInstanceOf[JServlet]

}
