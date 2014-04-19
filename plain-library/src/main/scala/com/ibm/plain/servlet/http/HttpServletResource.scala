package com.ibm

package plain

package servlet

package http

import javax.servlet.http.HttpServlet
import javax.servlet.http.Cookie
import javax.servlet.ServletConfig

import com.typesafe.config.Config

import scala.collection.mutable.HashMap

import aio.{ Exchange, ExchangeHandler }
import io.{ ByteArrayOutputStream, PrintWriter }
import plain.http.Response
import plain.http.Status.Success
import rest.{ Uniform, Context, StaticUniform }

final class HttpServletResource(

  private[this] final val servletwrapper: (Either[(Int, HttpServlet), HttpServlet], ServletConfig, ServletContext))

  extends Uniform

  with StaticUniform {

  final def init(config: Config) = ()

  final def process(exchange: Exchange[Context], handler: ExchangeHandler[Context]) = exchange.attachment match {
    case Some(context) ⇒ try {
      if (null == servlet) servlet = servletwrapper match {
        case (Right(servlet), servletconfig, _) ⇒
          servlet.init(servletconfig)
          servlet
        case (Left((_, servlet)), _, _) ⇒ servlet
      }
      val request = context.request
      val response = context.response
      response ++ new HashMap[String, String]
      val printwriter = exchange.printWriter match {
        case null ⇒
          exchange ++ PrintWriter(ByteArrayOutputStream(io.defaultBufferSize))
          exchange.printWriter
        case p ⇒
          p.outputstream.reset
          p
      }
      val parentloader = Thread.currentThread.getContextClassLoader
      Thread.currentThread.setContextClassLoader(servletcontext.getClassLoader)
      try {
        val httpservletrequest = new HttpServletRequest(request, exchange, servletcontext, servlet)
        val httpservletresponse = new HttpServletResponse(response, servletcontext, printwriter, servlet)
        servlet.service(httpservletrequest, httpservletresponse)
        response ++ Some(httpservletresponse.getEntity)
        if (httpservletrequest.hasSession) httpservletrequest.getSession match {
          case session if session.isNew ⇒
            val cookie = new Cookie("JSESSIONID", session.getId)
            cookie.setPath(request.path.take(2).mkString("/", "/", "/"))
            cookie.setHttpOnly(true)
            response ++ cookie
          case _ ⇒
        }
        completed(exchange, handler)
      } finally Thread.currentThread.setContextClassLoader(parentloader)
    } catch {
      case e: Throwable ⇒ failed(e, exchange, handler)
    }
    case _ ⇒
  }

  private[this] final var servlet: HttpServlet = servletwrapper match {
    case (Left((_, servlet)), _, _) ⇒ servlet
    case _ ⇒ null
  }

  private[this] final val servletcontext: ServletContext = servletwrapper._3

}
