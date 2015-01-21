package com.ibm.plain
package integration
package spaces

import java.io.{ FileInputStream, FileOutputStream }
import java.nio.file.Path

import org.apache.commons.io.FileUtils.deleteDirectory

import com.ibm.plain.bootstrap.{ ExternalComponent, Singleton }
import com.ibm.plain.integration.infrastructure.Infrastructure
import com.ning.http.client.{ AsyncHttpClient, RequestBuilder }

import aio.client.ClientExchange
import aio.conduit.{ AHCConduit, ChunkedConduit, FileConduit }
import bootstrap.{ ExternalComponent, Singleton }
import camel.Camel
import crypt.Uuid
import io.{ LZ4, copyBytes, temporaryDirectory }
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

      classOf[camel.Camel])

    with Logger {

  import SpacesClient._

  /**
   * PUT or upload to a container into the space.
   */
  final def put(

    name: String,

    container: Uuid,

    localdirectory: Path): Int = {

    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        val request = new RequestBuilder("PUT").
          setUrl(space.serverUri + "/" + container).
          setHeader("Transfer-Encoding", "chunked").
          setHeader("Expect", "100-continue").
          build
        val ahcconduit = AHCConduit(client, request)
        val tmpfile = packDirectory(localdirectory)
        trace(s"PUT started : ${request.getUrl}")
        ClientExchange(FileConduit.forReading(tmpfile), ChunkedConduit(ahcconduit)).transferAndWait
        trace(s"PUT finished: ${request.getUrl}")
        tmpfile.toFile.delete
        tmpfile.getParent.toFile.delete
        ahcconduit.getResponse match {
          case Some(response) ⇒ response.getStatusCode
          case _ ⇒ 501
        }
      case _ ⇒ illegalState(s"Trying to PUT a container to a non-existing space : $name")
    }
  }

  /**
   * POST a json with a list of containers and a list of files for each to the space.
   */
  final def post(name: String, content: String, localdirectory: Path, purgedirectory: Boolean): Int = {
    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) deleteDirectory(localdirectory.toFile)
        val request = new RequestBuilder("POST").
          setUrl(space.serverUri + "/00000000000000000000000000000000/").
          setHeader("ContentType", "application/json").
          setBody(content).
          build
        val lz4file = temporaryDirectory.toPath.resolve("lz4")
        val ahcconduit = AHCConduit(client, request)
        trace(s"POST started : ${request.getUrl}")
        ClientExchange(ahcconduit, FileConduit.forWriting(lz4file)).transferAndWait
        trace(s"POST finished : ${request.getUrl}")
        ahcconduit.getResponse match {
          case Some(response) if null != response && 200 == response.getStatusCode ⇒
            unpackDirectory(localdirectory, lz4file)
            200
          case Some(response) if null != response ⇒ response.getStatusCode
          case _ ⇒ 500
        }
      case _ ⇒ illegalState(s"Trying to POST to a non-existing space : $name")
    }
  }

  /**
   * GET or download from a container from the space into a local directory.
   */
  final def get(name: String, container: Uuid, localdirectory: Path, purgedirectory: Boolean): Int = {
    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) deleteDirectory(localdirectory.toFile)
        val request = new RequestBuilder("GET").
          setUrl(space.serverUri + "/" + container).
          build
        val lz4file = temporaryDirectory.toPath.resolve("lz4")
        val ahcconduit = AHCConduit(client, request)
        trace(s"GET started : ${request.getUrl}")
        ClientExchange(ahcconduit, FileConduit.forWriting(lz4file)).transferAndWait
        trace(s"GET finished : ${request.getUrl}")
        ahcconduit.getResponse match {
          case Some(response) if 200 == response.getStatusCode ⇒
            unpackDirectory(localdirectory, lz4file)
            200
          case Some(response) ⇒ response.getStatusCode
          case _ ⇒ 500
        }
      case _ ⇒ illegalState(s"Trying to GET a container from a non-existing space : $name")
    }
  }

  /**
   *
   */
  override final def start = {
    sys.addShutdownHook(ignore(stop))
    client = Camel.instance.httpClient
    SpacesClient.instance(this)
    this
  }

  override final def stop = {
    SpacesClient.resetInstance
    this
  }

  private[this] final var client: AsyncHttpClient = null

}

/**
 *
 */
object SpacesClient

    extends Singleton[SpacesClient] {

  /**
   * From tests the fastest combination is to store only (no-compression) with zip4j and then use fast lz4 compression.
   * Do not use high lz4 compression unless you have a really low bandwidth.
   */
  final def packDirectory(directory: Path): Path = {
    val tmpdir = temporaryDirectory.toPath
    val zfile = tmpdir.resolve("zip").toFile
    val zipfile = new ZipFile(zfile)
    val zipparameters = new ZipParameters
    zipparameters.setCompressionMethod(Zip4jConstants.COMP_STORE)
    zipparameters.setIncludeRootFolder(false)
    zipfile.addFolder(directory.toFile, zipparameters)
    val in = new FileInputStream(zfile)
    val lz4file = tmpdir.resolve("lz4").toFile
    val out = LZ4.fastOutputStream(new FileOutputStream(lz4file))
    try copyBytes(in, out)
    finally {
      in.close
      out.close
    }
    zfile.delete
    lz4file.toPath
  }

  final def unpackDirectory(directory: Path, lz4file: Path) = {
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
    deleteDirectory(file.toPath.getParent.toFile)
  }

}
