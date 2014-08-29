package com.ibm

package plain

package integration

package camel

import java.io.{ ByteArrayInputStream, File, FileOutputStream }
import java.nio.file.Files.{ createDirectories, exists }
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import org.apache.camel.impl.DefaultCamelContext

import bootstrap.{ ExternalComponent, Singleton }
import logging.Logger

/**
 *
 */
final class Camel

    extends ExternalComponent[Camel](

      camel.isEnabled,

      "plain-integration-camel")

    with Logger {

  import Camel._

  if (camel.isEnabled) enable else disable

  override def preStart = {
    createWarFile
    trace(servlet.webApplicationsDirectory + "/" + servletServicesRoot + ".war created.")
  }

  override def start = {
    context.start
    //CamelContext.
    Camel.instance(this)
    this
  }

  override def stop = {
    Camel.resetInstance
    context.stop
    ignore(Thread.sleep(delayDuringShutdown))
    this
  }

  final val context = new DefaultCamelContext

}

/**
 * Don't forget to call Camel.instance(this) in the companion class.
 */
object Camel

    extends Singleton[Camel] {

  private final def createWarFile = {
    val file = new File(servlet.webApplicationsDirectory + "/" + servletServicesRoot + ".war")
    io.createDirectory(file.toPath.getParent)
    val out = new JarOutputStream(new FileOutputStream(file))
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
