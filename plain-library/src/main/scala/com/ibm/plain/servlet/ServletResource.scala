package com.ibm

package plain

package servlet

import javax.servlet.http.Cookie

import scala.collection.mutable.HashMap

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
    super.failed(e, context.io ++ context.response.asInstanceOf[aio.Message])
  }

  final def process(io: Io) = unsupported

  final def handle(context: Context) = try {
    import context.io.printwriter
    val request = context.request
    val response = Response(request, Success.`200`, new HashMap[String, String])
    context ++ response
    if (null == printwriter) context.io ++ PrintWriter(ByteArrayOutputStream(io.defaultBufferSize)) else printwriter.outputstream.reset
    ServletContainer.getServletContext(request.path(1)) match {
      case null ⇒
        debug("404: " + request.path.mkString("/"))
        throw ClientError.`404`
      case servletcontext ⇒
        val classloader = Thread.currentThread.getContextClassLoader
        Thread.currentThread.setContextClassLoader(servletcontext.getClassLoader)
        try {
          servletcontext.getServlet(request.path(2)) match {
            case null ⇒
              val r = unpackWebApplicationsDirectory.getAbsolutePath + "/" + context.remainder.take(1).head
              val roots = List(r + "/WEB-INF/classes", r)
              response ++ rest.resource.DirectoryResource.get(roots, context.remainder.drop(1).mkString("/"))
              completed(context)
            case servlet ⇒
              val httpservletrequest = new HttpServletRequest(request, context, servletcontext)
              val httpservletresponse = new HttpServletResponse(response, servletcontext, printwriter)
              servlet.service(httpservletrequest, httpservletresponse)
              response ++ httpservletresponse.getEntity
              if (enableAutomaticCookieHandling) httpservletrequest.getSession match {
                case session if null != session && session.isNew ⇒
                  val cookie = new Cookie("JSESSIONID", session.getId)
                  cookie.setPath(request.path.take(2).mkString("/", "/", "/"))
                  cookie.setHttpOnly(true)
                  response ++ cookie
                case _ ⇒
              }
              completed(context)
          }
        } finally
          Thread.currentThread.setContextClassLoader(classloader)
    }
  } catch {
    case e: Throwable ⇒ failed(e, context)
  }

}
