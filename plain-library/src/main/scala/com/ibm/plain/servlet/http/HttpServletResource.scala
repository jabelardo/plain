package com.ibm

package plain

package servlet

package http

import javax.servlet.http.HttpServlet
import javax.servlet.http.Cookie

import scala.collection.mutable.HashMap

import aio.Io
import io.{ ByteArrayOutputStream, PrintWriter }
import plain.http.Response
import plain.http.Status.Success
import rest.{ BaseResource, Context, StaticResource }

final class HttpServletResource(

  private[this] final val servlet: HttpServlet)

  extends BaseResource

  with StaticResource {

  final def completed(context: Context) = super.completed(context.response, context.io)

  final def failed(e: Throwable, context: Context) = {
    super.failed(e, context.io ++ context.response.asInstanceOf[aio.Message])
  }

  final def process(io: Io) = unsupported

  final def handle(context: Context) = try {
    val request = context.request
    val response = Response(request, Success.`200`, new HashMap[String, String])
    context ++ response
    import context.io.printwriter
    if (null == printwriter) context.io ++ PrintWriter(ByteArrayOutputStream(io.defaultBufferSize)) else printwriter.outputstream.reset
    val parentloader = Thread.currentThread.getContextClassLoader
    Thread.currentThread.setContextClassLoader(classloader)
    try {
      val httpservletrequest = new HttpServletRequest(request, context, servletcontext, servlet)
      val httpservletresponse = new HttpServletResponse(response, servletcontext, printwriter, servlet)
      servlet.service(httpservletrequest, httpservletresponse)
      response ++ httpservletresponse.getEntity
      if (httpservletrequest.hasSession) httpservletrequest.getSession match {
        case session if session.isNew ⇒
          val cookie = new Cookie("JSESSIONID", session.getId)
          cookie.setPath(request.path.take(2).mkString("/", "/", "/"))
          cookie.setHttpOnly(true)
          response ++ cookie
        case _ ⇒
      }
      completed(context)
    } finally Thread.currentThread.setContextClassLoader(parentloader)
  } catch {
    case e: Throwable ⇒ failed(e, context)
  }

  private[this] final val servletcontext = servlet.getServletContext.asInstanceOf[ServletContext]

  private[this] final val classloader = servletcontext.getClassLoader

}
