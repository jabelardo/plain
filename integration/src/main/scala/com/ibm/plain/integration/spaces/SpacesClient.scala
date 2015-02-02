package com.ibm.plain
package integration
package spaces

import java.io.{ File, FileInputStream, FileOutputStream }
import java.nio.file.Path

import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.http.client.methods.{ HttpGet, HttpPost, HttpPut }
import org.apache.http.entity.{ FileEntity, StringEntity }
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.client.config.RequestConfig
import org.apache.http.NoHttpResponseException

import com.ibm.plain.bootstrap.{ ExternalComponent, Singleton }
import com.ibm.plain.integration.infrastructure.Infrastructure
import com.ning.http.client.{ AsyncHttpClient, RequestBuilder }

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
        val put = new HttpPut(space.serverUri + "/" + container)
        try {
          trace(s"PUT pack directory : localdirectory = $localdirectory")
          val lz4file = packDirectory(localdirectory)
          val config = RequestConfig.
            custom.
            setConnectTimeout(defaulttimeout).
            setConnectionRequestTimeout(defaulttimeout).
            setSocketTimeout(defaulttimeout).
            build
          val client = HttpClientBuilder.create.setDefaultRequestConfig(config).build
          put.setHeader("Expect", "100-continue")
          put.setEntity(new FileEntity(lz4file.toFile))
          try {
            trace(s"PUT started : uri = ${put.getURI}")
            val response = client.execute(put)
            trace(s"PUT finished : statuscode = '${response.getStatusLine}', uri = ${put.getURI}")
            response.close
            201
          } catch {
            case e: NoHttpResponseException ⇒
              trace(s"PUT finished : uri = ${put.getURI}")
              201
            case e: Throwable ⇒
              error(s"PUT failed : uri = ${put.getURI}\n$e")
              501
          } finally {
            client.close
            lz4file.toFile.delete
            lz4file.getParent.toFile.delete
          }
        } catch {
          case e: Throwable ⇒
            error(s"PUT failed : uri = ${put.getURI}\n$e")
            501
        }
      case _ ⇒ illegalState(s"Trying to PUT a container to a non-existing space : $name")
    }
  }

  /**
   * GET or download from a container from the space into a local directory.
   */
  final def get(name: String, container: Uuid, localdirectory: Path, purgedirectory: Boolean): Int = {
    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) deleteDirectory(localdirectory.toFile)
        val config = RequestConfig.
          custom.
          setConnectTimeout(defaulttimeout).
          setConnectionRequestTimeout(defaulttimeout).
          setSocketTimeout(defaulttimeout).
          build
        val client = HttpClientBuilder.create.setDefaultRequestConfig(config).build
        val get = new HttpGet(space.serverUri + "/" + container)
        try {
          trace(s"GET started : ${get.getURI}")
          val response = client.execute(get)
          val in = response.getEntity.getContent
          val lz4file = temporaryDirectory.toPath.resolve("lz4").toFile
          val out = new FileOutputStream(lz4file)
          try copyBytes(in, out)
          finally {
            in.close
            out.close
            response.close
          }
          unpackDirectory(localdirectory, lz4file)
          trace(s"GET finished : ${get.getURI}")
          200
        } catch {
          case e: Throwable ⇒
            error(s"GET failed : ${get.getURI}\n$e")
            401
        } finally {
          client.close
        }
      case _ ⇒ illegalState(s"Trying to GET a container from a non-existing space : $name")
    }
  }

  /**
   * POST a json with a list of containers and a list of files for each to the space.
   */
  final def post(name: String, content: String, localdirectory: Path, purgedirectory: Boolean): Int = {
    spaceslist.find(name == _.name) match {
      case Some(space) ⇒
        if (purgedirectory) deleteDirectory(localdirectory.toFile)
        val config = RequestConfig.
          custom.
          setConnectTimeout(defaulttimeout).
          setConnectionRequestTimeout(defaulttimeout).
          setSocketTimeout(defaulttimeout).
          build
        val client = HttpClientBuilder.create.setDefaultRequestConfig(config).build
        val post = new HttpPost(space.serverUri + "/00000000000000000000000000000000/")
        post.setHeader("ContentType", "application/json")
        post.setEntity(new StringEntity(content))
        val lz4file = temporaryDirectory.toPath.resolve("lz4").toFile
        try {
          trace(s"POST started : uri = ${post.getURI}")
          val response = client.execute(post)
          val in = response.getEntity.getContent
          val out = new FileOutputStream(lz4file)
          try copyBytes(in, out)
          finally {
            in.close
            out.close
            response.close
          }
          unpackDirectory(localdirectory, lz4file)
          trace(s"POST finished : statuscode = '${response.getStatusLine}', uri = ${post.getURI}")
          200
        } catch {
          case e: Throwable ⇒
            error(s"POST failed : uri = ${post.getURI}\n$e")
            401
        } finally {
          client.close
        }
      case _ ⇒ illegalState(s"Trying to POST to a non-existing space : $name")
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

  private[this] final val defaulttimeout = 90000

}

/**
 *
 */
object SpacesClient

    extends Singleton[SpacesClient]

    with Logger {

  /**
   * From tests the fastest combination is to store only (no-compression) with zip4j and then use fast lz4 compression.
   * Do not use high lz4 compression unless you have a really low bandwidth.
   *
   * BUT: lz4 sucks with sizes larger than 2gb, what crap!
   *
   */
  final def packDirectory(directory: Path): Path = {
    val tmpdir = temporaryDirectory.toPath
    val zfile = tmpdir.resolve("zip").toFile
    trace(s"packDirectory started : ${zfile.getAbsolutePath}")
    val zipfile = new ZipFile(zfile)
    val zipparameters = new ZipParameters
    zipparameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE)
    zipparameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_FASTEST)
    zipparameters.setIncludeRootFolder(false)
    zipfile.addFolder(directory.toFile, zipparameters)
    trace(s"packDirectory finished : ${zfile.getAbsolutePath} length = ${zfile.length}")
    zfile.toPath
  }

  final def unpackDirectory(directory: Path, lz4file: File) = {
    trace(s"unpackDirectory started : ${lz4file.getAbsolutePath} length = ${lz4file.length}")
    try {
      val file = lz4file.toPath.getParent.resolve("zip").toFile
      val in = LZ4.inputStream(new FileInputStream(lz4file))
      val out = new FileOutputStream(file)
      try copyBytes(in, out)
      finally {
        in.close
        out.close
      }
      trace(s"unpackDirectory : extracted lz4 to zip : ${file.getAbsolutePath} length = ${file.length}")
      try {
        val zipfile = new ZipFile(file)
        zipfile.extractAll(directory.toFile.getAbsolutePath)
      } catch {
        case e: Throwable ⇒
          error(s"unpackDirectory failed : File is corrupted due to legacy size limitations : $lz4file $e")
          illegalState(s"Could not load from spaces, file is corrupted due to legacy size limitations: $lz4file")
      }
    } catch {
      case e: Throwable ⇒
        warn(s"unpackDirectory : Probably not an lz4 file (trying unzip now) : ${lz4file.getAbsolutePath} : $e")
        val zipfile = new ZipFile(lz4file)
        zipfile.extractAll(directory.toFile.getAbsolutePath)
    }
    trace(s"unpackDirectory finished : target directory = ${directory.toFile.getAbsolutePath}")
    deleteDirectory(lz4file.toPath.getParent.toFile)
  }

}
