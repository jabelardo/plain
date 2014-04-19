package com.ibm

package plain

package integration

package camel

import java.io.{ ByteArrayInputStream, FileOutputStream }
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import scala.concurrent.duration.Duration

import akka.actor.ActorSystem
import akka.camel.{ Camel ⇒ AkkaCamel, CamelExtension }
import bootstrap.ExternalComponent
import logging.Logger

/**
 *
 */
final class Camel

  extends ExternalComponent[Camel]("plain-integration-camel")

  with Logger {

  import Camel._

  override def order = bootstrapOrder

  override def start = {
    if (null == actorsystem && null == camel) {
      actorsystem = ActorSystem(actorSystemName, defaultExecutionContext = Some(concurrent.executor))
      camel = CamelExtension(actorsystem)
      camelextension = camel
    }
    this
  }

  override def stop = {
    if (null != actorsystem && null != camel) {
      actorsystem.shutdown
      actorsystem = null
      camel = null
      camelextension = null
      ignore(Thread.sleep(delayDuringShutdown))
    }
    this
  }

  override final def awaitTermination(timeout: Duration) = actorsystem.awaitTermination(timeout)

  private[this] final var actorsystem: ActorSystem = null

  private[this] final var camel: AkkaCamel = null

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
