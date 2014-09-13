package com.ibm.plain
package integration
package spaces

import java.io.{ File, FileInputStream, FileOutputStream }
import java.nio.file.Path

import org.apache.commons.io.FileUtils.deleteDirectory

import com.ibm.plain.bootstrap.{ ExternalComponent, Singleton }
import com.ibm.plain.integration.infrastructure.Infrastructure
import com.ning.http.client.{ AsyncHttpClient, AsyncHttpClientConfig, RequestBuilder }
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider

import aio.client.ClientExchange
import aio.conduit.{ AHCConduit, ChunkedConduit, FileConduit, GzipConduit, TarConduit }
import bootstrap.{ ExternalComponent, Singleton }
import crypt.Uuid
import io.{ temporaryFile, temporaryDirectory, LZ4, copyBytes }
import logging.Logger
import net.lingala.zip4j.core.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.util.Zip4jConstants

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
  final def put(name: String, container: Uuid, localdirectory: Path, useConduit: Boolean): Int = {

    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        val url = space.serverUri + "/" + container
        val (source, destination, ahcconduit, tmpfile) = if (useConduit) {
          val request = new RequestBuilder("PUT").
            setUrl(space.serverUri + "/" + container).
            setHeader("Transfer-Encoding", "chunked").
            setHeader("Expect", "100-continue").
            build
          val ahcconduit = AHCConduit(client, request)
          (TarConduit(localdirectory.toFile), GzipConduit(ChunkedConduit(ahcconduit)), ahcconduit, None)
        } else {
          val tmpfile = packDirectory(localdirectory)
          val request = new RequestBuilder("PUT").
            setUrl(space.serverUri + "/" + container).
            setHeader("Transfer-Encoding", "chunked").
            setHeader("Expect", "100-continue").
            build
          val ahcconduit = AHCConduit(client, request)
          (FileConduit.forReading(tmpfile), ChunkedConduit(ahcconduit), ahcconduit, Some(tmpfile))
        }
        ClientExchange(source, destination).transferAndWait
        tmpfile.map(_.toFile.delete)
        ahcconduit.getResponse match {
          case Some(response) ⇒ response.getStatusCode
          case _ ⇒ 501
        }
      case _ ⇒ illegalState(s"Trying to PUT a container to a non-existing space : $name")
    }
  }

  /**
   * GET or download from a container from the space into a local directory.
   */
  final def get(name: String, container: Uuid, localdirectory: Path, purgedirectory: Boolean, useConduit: Boolean): Int = {

    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) {
          ignore(deleteDirectory(localdirectory.toFile))
          debug("Purged localdirectory : " + localdirectory)
        }
        val url = space.serverUri + "/" + container
        val request = new RequestBuilder("GET").
          setUrl(space.serverUri + "/" + container).
          build
        val ahcconduit = AHCConduit(client, request)
        val (source, destination, tmpfile) = if (useConduit) {
          (GzipConduit(ahcconduit), TarConduit(localdirectory.toFile), None)
        } else {
          val lz4file = temporaryDirectory.toPath.resolve("lz4")
          (ahcconduit, FileConduit.forWriting(lz4file), Some(lz4file))
        }
        ClientExchange(source, destination).transferAndWait
        if (!useConduit) unpackDirectory(localdirectory, tmpfile.get)
        ahcconduit.getResponse match {
          case Some(response) ⇒ response.getStatusCode
          case _ ⇒ 501
        }
      case _ ⇒ illegalState(s"Trying to GET a container from a non-existing space : $name")
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

  private[this] final def packDirectory(directory: Path): Path = {
    val tmpdir = temporaryDirectory.toPath
    val zfile = tmpdir.resolve("zip").toFile
    val zipfile = new ZipFile(zfile)
    val zipparameters = new ZipParameters
    zipparameters.setCompressionMethod(Zip4jConstants.COMP_STORE)
    zipparameters.setIncludeRootFolder(false)
    zipfile.addFolder(directory.toFile, zipparameters)
    val in = new FileInputStream(zfile)
    val lz4file = tmpdir.resolve("lz4").toFile
    val out = LZ4.highOutputStream(new FileOutputStream(lz4file))
    try copyBytes(in, out)
    finally {
      in.close
      out.close
    }
    zfile.delete
    lz4file.toPath
  }

  private[this] final def unpackDirectory(directory: Path, lz4file: Path) = {
    val file = lz4file.getParent.resolve("zip").toFile
    val in = LZ4.inputStream(new FileInputStream(lz4file.toFile))
    val out = new FileOutputStream(file)
    try copyBytes(in, out)
    finally {
      in.close
      out.close
    }
    lz4file.toFile.delete
    val zipfile = new ZipFile(file)
    zipfile.extractAll(directory.toFile.getAbsolutePath)
    file.delete
  }

  private[this] final var client: AsyncHttpClient = null

}

/**
 *
 */
object SpacesClient

  extends Singleton[SpacesClient]
