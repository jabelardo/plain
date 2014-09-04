package com.ibm.plain
package integration
package spaces

import java.nio.file.Path

import com.ibm.plain.bootstrap.{ ExternalComponent, Singleton }
import com.ibm.plain.integration.infrastructure.Infrastructure
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, RequestBuilder }
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider
import org.apache.commons.io.FileUtils.deleteDirectory

import aio.client.ClientExchange
import aio.conduit.{ AHCConduit, ChunkedConduit, GzipConduit, TarConduit }
import bootstrap.{ ExternalComponent, Singleton }
import crypt.Uuid
import logging.Logger

/**
 *
 */
final class SpacesClient

    extends ExternalComponent[SpacesClient](

      isClientEnabled,

      "plain-integration-spaces-client",

      classOf[infrastructure.Infrastructure])

    with Logger {

  /**
   * PUT or upload to a container into the space.
   */
  final def put(

    name: String,

    container: Uuid,

    localdirectory: Path): Int = {

    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        val url = space.serverUri + "/" + container
        val request = new RequestBuilder("PUT").
          setUrl(space.serverUri + "/" + container).
          setHeader("Transfer-Encoding", "chunked").
          setHeader("Expect", "100-continue").
          build
        val source = TarConduit(localdirectory.toFile)
        val ahcconduit = AHCConduit(client, request)
        val destination = GzipConduit(ChunkedConduit(ahcconduit))
        ClientExchange(source, destination).transferAndWait
        ahcconduit.getResponse match {
          case Some(response) ⇒ response.getStatusCode
          case _ ⇒ 501
        }
      case _ ⇒
        throw new IllegalStateException(s"Trying to PUT a container to a non-existing space : $name")
    }
  }

  /**
   * GET or download from a container from the space into a local directory.
   */
  final def get(

    name: String,

    container: Uuid,

    localdirectory: Path,

    purgedirectory: Boolean): Int = {

    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) {
          ignore(deleteDirectory(localdirectory.toFile))
          warn("Purged local directory : " + localdirectory)
        }
        val url = space.serverUri + "/" + container
        val request = new RequestBuilder("GET").
          setUrl(space.serverUri + "/" + container).
          build
        val ahcconduit = AHCConduit(client, request)
        val source = GzipConduit(ahcconduit)
        val destination = TarConduit(localdirectory.toFile)
        ClientExchange(source, destination).transferAndWait
        ahcconduit.getResponse match {
          case Some(response) ⇒
            error(request.getMethod + " " + request.getUrl + " : " + response.getStatusCode)
            response.getStatusCode
          case _ ⇒
            error("No response.")
            501
        }
      case _ ⇒
        throw new IllegalStateException(s"Trying to GET a container from a non-existing space : $name")
    }

  }

  /**
   *
   */
  override final def start = {
    val timeout = requestTimeout
    val config = new AsyncHttpClientConfig.Builder().
      setRequestTimeoutInMs(timeout).
      setConnectionTimeoutInMs(timeout).
      setIdleConnectionTimeoutInMs(timeout).
      setIdleConnectionInPoolTimeoutInMs(timeout).
      build
    client = new AsyncHttpClient(new NettyAsyncHttpProvider(config))
    SpacesClient.instance(this)
    sys.addShutdownHook(ignore(stop))
    this
  }

  override final def stop = {
    ignore(client.close)
    client = null
    Spaces.resetInstance
    this
  }

  private[this] final var client: AsyncHttpClient = null

}

/**
 *
 */
object SpacesClient

  extends Singleton[SpacesClient]
