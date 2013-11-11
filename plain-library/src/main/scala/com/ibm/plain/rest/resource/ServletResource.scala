package com.ibm

package plain

package rest

package resource

import scala.collection.JavaConversions._

import aio.Io
import http.Status._
import servlet._

final class ServletResource

  extends BaseResource {

  final def completed(context: Context) = {
    info("completed")
  }

  final def failed(e: Throwable, context: Context) = {
    e.printStackTrace
  }

  final def process(io: Io) = unsupported

  final def handle(context: Context) = try {
    info("handle " + context.request)
    ServletContainer.getServletContext(context.request.path(1)) match {
      case Some(servletcontext) ⇒
        info(servletcontext.getServletNames.toList.toString)
        val servlet = servletcontext.getServlet(context.request.path(2))
        info(servlet.getServletInfo)
        servlet.service(null, null)
        throw ServerError.`501`
      case None ⇒ throw ClientError.`404`
    }
  } catch {
    case e: Throwable ⇒ failed(e, context)
  }

}
