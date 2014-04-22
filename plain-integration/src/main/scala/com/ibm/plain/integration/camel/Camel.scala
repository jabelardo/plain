package com.ibm

package plain

package integration

package camel

import java.io.{ ByteArrayInputStream, FileOutputStream }
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import org.apache.camel.impl.DefaultCamelContext

import bootstrap.ExternalComponent
import logging.Logger

/**
 *
 */
final class Camel

  extends ExternalComponent[Camel]("plain-integration-camel")

  with Logger {

  import Camel.createWarFile

  override def order = bootstrapOrder

  override def start = {
    if (null == camelcontext) {
      camelcontext = new DefaultCamelContext
      camelcontext.start
    }
    this
  }

  override def stop = {
    if (null != camelcontext) {
      camelcontext.stop
      camelcontext = null
      ignore(Thread.sleep(delayDuringShutdown))
    }
    this
  }

  /*
   * Must be done at creation time to be used by the ServletContainer.
   */
  createWarFile

}

object Camel {

  private final def createWarFile = {
    val out = new JarOutputStream(new FileOutputStream(servlet.webApplicationsDirectory + "/" + servletServicesRoot + ".war"))
    out.putNextEntry(new ZipEntry("WEB-INF/web.xml"))
    val in = new ByteArrayInputStream(webxml)
    io.copyBytes(in, out)
    in.close
    out.closeEntry
    out.close
  }

  private[this] final val webxml = """<web-app>
	<servlet>
		<servlet-name>CamelServlet</servlet-name>
		<display-name>plain-integration-camel-servlet</display-name>
		<servlet-class>org.apache.camel.component.servlet.CamelHttpTransportServlet
		</servlet-class>
	</servlet>
	<servlet-mapping>
		<servlet-name>CamelServlet</servlet-name>
		<url-pattern>/*</url-pattern>
	</servlet-mapping>
</web-app>
""".getBytes("UTF-8")

}
