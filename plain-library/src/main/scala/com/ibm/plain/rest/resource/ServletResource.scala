package com.ibm

package plain

package rest

package resource

import com.ibm.plain.rest.{ BaseResource, Context }

import io.{ ByteArrayOutputStream, PrintWriter }
import aio.Io
import http.Response
import http.Status.{ ClientError, Success }
import servlet.ServletContainer
import servlet.http.{ HttpServletRequest, HttpServletResponse }

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
    if (null == context.io.printwriter) context.io ++ PrintWriter(ByteArrayOutputStream(1024)) else context.io.printwriter.getOutputStream.reset
    ServletContainer.getServletContext(request.path(1)) match {
      case Some(servletcontext) ⇒
        val classloader = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(servletcontext.getClassLoader)
        try {
          servletcontext.getServlet(request.path(2)) match {
            case null ⇒ throw ClientError.`404`
            case servlet ⇒
              val httpservletrequest = new HttpServletRequest(request, servletcontext)
              val httpservletresponse = new HttpServletResponse(response, servletcontext, context.io.printwriter)
              servlet.service(httpservletrequest, httpservletresponse)
              response ++ httpservletresponse.getEntity
              completed(context)
          }
        } finally Thread.currentThread.setContextClassLoader(classloader)
      case None ⇒ throw ClientError.`404`
    }
  } catch {
    case e: Throwable ⇒ failed(e, context)
  }

}

object ServletResource {

  // final val entity = http.Entity.ArrayEntity("Hello, World!".getBytes(text.`UTF-8`), http.ContentType(http.MimeType.`text/plain`, text.`UTF-8`))

}
