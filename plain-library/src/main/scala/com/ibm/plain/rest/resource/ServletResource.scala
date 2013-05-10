package com.ibm

package plain

package rest

package resource

import javax.servlet.Servlet

import rest.Resource
import servlet._

final class ServletResource

  extends Resource {

  Get {
    val servlet = Class.forName("com.vaadin.terminal.gwt.server.ApplicationServlet").newInstance.asInstanceOf[Servlet]
    val servletconfig = ServletConfig.apply
    servletconfig.setInitParameter("application", "com.vaadin.demo.sampler.SamplerApplication")
    servlet.init(servletconfig)
    val servletrequest = HttpServletRequest(context)
    val servletresponse = HttpServletResponse(context)
    servlet.service(servletrequest, servletresponse)
    42.toString.getBytes
  }

}

