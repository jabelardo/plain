package com.ibm

package plain

package servlet

package http

import javax.servlet._
import javax.servlet.http.{ HttpServlet ⇒ JHttpServlet }
import javax.servlet.http._

class HttpServlet

  extends JHttpServlet {

}

class A extends HttpServlet {

  override protected def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    println("doGet")
  }

  override def service(request: ServletRequest, response: ServletResponse) = {
    doGet(null, null)
  }

}

object Test1 extends App {
  val a = new A
  a.init
  println(a)
  // println(a.getInitParameterNames)
  println(a.getServletConfig)
  // println(a.getServletContext)
  println(a.getServletInfo)
  // println(a.getServletName)
  println(a.service(null, null))

}
