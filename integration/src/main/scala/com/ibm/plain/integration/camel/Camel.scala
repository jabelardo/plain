package com.ibm.plain
package integration
package camel

import java.io.{ ByteArrayInputStream, File, FileOutputStream }
import java.util.concurrent.TimeUnit
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.camel.component.ahc.AhcComponent
import org.apache.camel.impl.DefaultCamelContext
import org.apache.commons.io.FileUtils.deleteDirectory

import com.ibm.plain.bootstrap.{ ExternalComponent, Singleton }
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig }
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider

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
    context.getShutdownStrategy.setShutdownNowOnTimeout(true)
    context.getShutdownStrategy.setTimeout(shutdownTimeout)
    context.start
    Camel.instance(this)
    sys.addShutdownHook(ignore(stop))
    this
  }

  override def stop = {
    Camel.resetInstance
    context.getRouteDefinitions.map(r ⇒ context.stopRoute(r.getId, shutdownTimeout, TimeUnit.MILLISECONDS, true))
    context.stop
    deleteWarFile
    ignore(client.doClose)
    ignore(Thread.sleep(delayDuringShutdown))
    this
  }

  final def httpClient: AsyncHttpClient = client

  final val context = new DefaultCamelContext

  private trait HasDoClose { def doClose }

  private[this] final val client: AsyncHttpClient with HasDoClose = {
    val config = new AsyncHttpClientConfig.Builder().
      setRequestTimeoutInMs(requestTimeout).
      setConnectionTimeoutInMs(requestTimeout).
      setIdleConnectionTimeoutInMs(requestTimeout).
      setIdleConnectionInPoolTimeoutInMs(requestTimeout).
      setAllowPoolingConnection(true).
      setFollowRedirects(true).
      setMaxRequestRetry(5).
      setMaximumConnectionsPerHost(100).
      setMaximumConnectionsTotal(256).
      build
    val client = new AsyncHttpClient(new NettyAsyncHttpProvider(config)) with HasDoClose {
      override def close = error("You are not allowed do call close() on AsyncHttpClient.")
      def doClose = super.close
    }
    val ahccomponent = context.getComponent("ahc", classOf[AhcComponent])
    ahccomponent.setClient(client)
    client
  }

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

  private final def deleteWarFile = {
    new File(servlet.webApplicationsDirectory + "/" + servletServicesRoot + ".war").delete
    deleteDirectory(new File(servlet.webApplicationsDirectory + "/" + servletServicesRoot))
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
