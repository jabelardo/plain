package com.ibm

package plain

package rest

package resource

import javax.servlet.Servlet

import servlet._

final class ServletResource

  extends Resource {

  def get = {
    val servlet = Class.forName("com.vaadin.terminal.gwt.server.ApplicationServlet").newInstance.asInstanceOf[Servlet]
    val servletconfig = ServletConfig.apply
    servletconfig.setInitParameter("application", "com.vaadin.demo.sampler.SamplerApplication")
    servlet.init(servletconfig)
    val servletrequest = HttpServletRequest(context)
    val servletresponse = HttpServletResponse(context)
    servlet.service(servletrequest, servletresponse)
    val s = servletresponse.getContentType
    s
  }

  Get { get }

  Get { query: String ⇒ get }

  Post { s: String ⇒ get }

}

