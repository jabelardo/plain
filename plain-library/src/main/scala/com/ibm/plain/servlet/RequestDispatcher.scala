package com.ibm

package plain

package servlet

import javax.{ servlet ⇒ js }
import js.RequestDispatcher._

final class RequestDispatcher(

  private[this] final val path: String,

  private[this] final val servletcontext: ServletContext)

  extends js.RequestDispatcher {

  final def forward(request: js.ServletRequest, response: js.ServletResponse) = {
    val req = request.asInstanceOf[js.http.HttpServletRequest]
    request.setAttribute(INCLUDE_REQUEST_URI, req.getRequestURI)
    request.setAttribute(FORWARD_CONTEXT_PATH, req.getContextPath)
    request.setAttribute(FORWARD_PATH_INFO, req.getPathInfo)
    request.setAttribute(FORWARD_QUERY_STRING, req.getQueryString)
    request.setAttribute(FORWARD_REQUEST_URI, req.getRequestURI)
    request.setAttribute(FORWARD_SERVLET_PATH, req.getServletPath)
    request.setAttribute("org.apache.catalina.jsp_file", path)
    servletcontext.getJspServlet.service(request, response)
  }

  final def include(request: js.ServletRequest, response: js.ServletResponse) = {

  }

}
