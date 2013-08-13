package com.ibm

package plain

package rest

package resource

import javax.servlet.Servlet

import servlet._

final class ServletResource

  extends Resource {

  final private def get = {
    val servlet = Class.forName("com.vaadin.terminal.gwt.server.ApplicationServlet").newInstance.asInstanceOf[Servlet]
    val servletconfig = ServletConfig(context)
    servletconfig.setInitParameter("application", "com.vaadin.demo.sampler.SamplerApplication")
    //    servletconfig.setInitParameter("widgetset", "com.vaadin.demo.sampler.gwt.SamplerWidgetSet")
    servletconfig.setInitParameter("productionMode", "true")
    servletconfig.setInitParameter("resourceCacheTime", "3600")
    servlet.init(servletconfig)
    val servletrequest = HttpServletRequest(context)
    val servletresponse = HttpServletResponse(context)
    servlet.service(servletrequest, servletresponse)
    servletresponse.toString
  }

  Get { get }

  Get { query: String ⇒ get }

  Post { f: String ⇒ println("POST " + f); get }

}

