package com.ibm

package plain

package servlet

import javax.{ servlet ⇒ js }
import plain.io.{ ByteArrayOutputStream, PrintWriter }
import plain.http.Status.ClientError.`404`

final case class RequestDispatcher(

  private final val path: String,

  private final val servletcontext: ServletContext)

  extends js.RequestDispatcher {

  final def forward(request: js.ServletRequest, response: js.ServletResponse) = {
    val req = request.asInstanceOf[js.http.HttpServletRequest]
    request.setAttribute(js.RequestDispatcher.INCLUDE_REQUEST_URI, req.getRequestURI)
    request.setAttribute(js.RequestDispatcher.FORWARD_CONTEXT_PATH, req.getContextPath)
    request.setAttribute(js.RequestDispatcher.FORWARD_PATH_INFO, req.getPathInfo)
    request.setAttribute(js.RequestDispatcher.FORWARD_QUERY_STRING, req.getQueryString)
    request.setAttribute(js.RequestDispatcher.FORWARD_REQUEST_URI, req.getRequestURI)
    request.setAttribute(js.RequestDispatcher.FORWARD_SERVLET_PATH, req.getServletPath)
    request.setAttribute("org.apache.catalina.jsp_file", path)
    servletcontext.getJspServlet.service(request, response)
  }

  final def include(request: js.ServletRequest, response: js.ServletResponse) = {

  }

}
