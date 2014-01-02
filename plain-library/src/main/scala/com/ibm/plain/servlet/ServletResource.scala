package com.ibm

package plain

package servlet

import aio.Io
import http.{ HttpServletRequest, HttpServletResponse }
import io.{ ByteArrayOutputStream, PrintWriter }
import rest.{ BaseResource, Context }
import plain.http.Response
import plain.http.Status.{ ClientError, Success }

final class ServletResource

  extends BaseResource {

  final def completed(context: Context) = super.completed(context.response, context.io)

  final def failed(e: Throwable, context: Context) = {
    e.printStackTrace
    super.failed(e, context.io ++ context.response.asInstanceOf[aio.Message])
  }

  final def process(io: Io) = unsupported

  final def handle(context: Context) = try {
    val request = context.request
    val response = Response(request, Success.`200`)
    context ++ response
    import context.io._
    if (null == printwriter)
      context.io ++ PrintWriter(ByteArrayOutputStream(io.defaultBufferSize))
    else
      printwriter.outputstream.reset
    ServletContainer.getServletContext(request.path(1)) match {
      case Some(servletcontext) ⇒
        val classloader = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(servletcontext.getClassLoader)
        try {
          servletcontext.getServlet(request.path(2)) match {
            case null ⇒ throw ClientError.`404`
            case servlet ⇒
              val httpservletrequest = new HttpServletRequest(request, servletcontext)
              val httpservletresponse = new HttpServletResponse(response, servletcontext, printwriter)
              servlet.service(httpservletrequest, httpservletresponse)
              response ++ httpservletresponse.getEntity
              completed(context)
          }
        } finally
          Thread.currentThread.setContextClassLoader(classloader)
      case None ⇒ throw ClientError.`404`
    }
  } catch {
    case e: Throwable ⇒ failed(e, context)
  }

}
