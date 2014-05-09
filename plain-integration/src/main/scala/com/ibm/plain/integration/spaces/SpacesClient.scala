package com.ibm

package plain

package integration

package spaces

import java.io._
import java.nio.file.{ FileSystems, Files, Path }
import java.util.UUID

import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveOutputStream, TarArchiveInputStream}
import org.apache.http.{ HttpHeaders, HttpResponse }
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.{HttpPut, HttpDelete, HttpGet, HttpRequestBase}

import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader

import rest.StaticResource
import io.deleteDirectory
import logging.Logger
import org.apache.http.entity.InputStreamEntity
import java.util.concurrent.{FutureTask, Callable}
import scala.concurrent._
import scala.concurrent.duration._
import scala.Some
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 *
 */
final class SpacesClient private {

  import SpacesClient._ // just in case we need the logger methods for instance

}

final case class SpacesURI(space: String, container: String = UUID.randomUUID.toString) {

  /** Returns the url of the server where the spaces server component is running (without trailing '/') */
  private lazy val hostSpacesURL = {
    // TODO: Get the real server name via infrastructure component
    // "localhost:7070/spaces"
    "harryklein.munich.de.ibm.com:7070/spaces/$space"
  }

  /** Returns the unified resource name */
  lazy val urn = {
    s"spaces://$space/$container"
  }

  /** Returns the unified resource location (without trailing '/') */
  lazy val url = {
    s"http://$hostSpacesURL/$container"
  }

  override def toString() = {
    super.toString() + s"[url = $url]"
  }

}

/**
 *
 */
object SpacesClient

  extends Logger {

  private object PredefinedHeaders {

    val AcceptEncoding = new BasicHeader(HttpHeaders.ACCEPT_ENCODING, downloadEncoding)

  }

  import PredefinedHeaders._

  /**
   * Downloads a resource from the given URI.
   *
   * @param uri
   * The URI of the container which content should be downloaded.
   * @param path
   * The file where the download should be stored.
   * @param relativePath
   * The relative path of the resource within the container.
   * @param fileName
   * Optional name of the file when it is stored on local file system. If None, the real name (served by the server) will be used.
   * If the file already exists. It will be deleted.
   * @return
   * Unit
   */
  final def get(uri: SpacesURI, path: Path, relativePath: Option[Path] = None, fileName: Option[String] = None) = httpRequest(new HttpGet(url(uri, relativePath)), AcceptEncoding) {
    case (200, response) ⇒
      require(Files.isDirectory(path), "The path must be an existing directory.")

      response.getAllHeaders.toList.foreach {
        case header ⇒
          debug(header.getName + " -> " + header.getValue)
      }

      // Figure out the target directory
      val localFilePath = path.resolve(fileName match {
        case Some(name) ⇒ name
        case None ⇒ response.getHeaders("Content-Disposition").toList.headOption match {
          case Some(value) ⇒ value.getValue
          case None ⇒
            // TODO: Throw exception - Muss von SpacesServer mit geliefert werden.
            "undefined"
        }
      })

      // Clean & Create local directory
      deleteDirectory(localFilePath.toFile, false)
      Files.createDirectories(localFilePath)

      // TAR entpcken
      // TODO: TAR spezifisches in TAR Utils auslagern?
      val in = new TarArchiveInputStream(response.getEntity.getContent)

      while (in.getNextEntry != null) {
        val entry = in.getCurrentEntry
        val targetFile = localFilePath.resolve(entry.getName)
        if (entry.isDirectory) {
          debug(s"Untar directory ${targetFile.toAbsolutePath}")
          if (!Files.exists(targetFile)) {
            debug(s"Create directory ${targetFile.toAbsolutePath}")
            Files.createDirectory(targetFile)
          }
        } else {
          debug(s"Untar ${targetFile.toAbsolutePath}")
          val out = new FileOutputStream(targetFile.toFile)
          io.copyBytes(in, out)
          out.close()
        }
      }

      debug("GET Done.")
    case (code, response) ⇒
      // TODO: Throw exception
      error(s"Unable to handle $code.")
  }

  /**
   * Uploads a resource to the given URI.
   *
   * @param uri
   * The target URI.
   * @param file
   * The file or directory which should be uploaded.
   * @param relativePath
   * The relative path within the container.
   * @return
   * Unit
   */
  final def put(uri: SpacesURI, file: Path, relativePath: Option[Path] = None) = {
    require(Files.isDirectory(file), "The path must be an existing directory.")

    val request = new HttpPut(url(uri, relativePath))

    val pos = new PipedOutputStream()
    val pis = new PipedInputStream(pos)

    val createTar = Future {
        val out = new TarArchiveOutputStream(pos)
        file.toFile.listFiles.toList.foreach(addFilesToTar(out, _, FileSystems.getDefault.getPath(".")))
        out.close()
    }

    val startRequest = Future {
        val entity = new InputStreamEntity(pis)
        entity.setChunked(true)
        request.setEntity(entity)

        httpRequest(request) {
          case (200, response) =>

        }
    }

    startRequest.onComplete {
      case Success(_) =>
        trace("PUT was successful")
      case Failure(t) =>
        error("An error occured... " + t.getMessage)
    }

    Await.result(startRequest, 5 minutes)
  }

  /**
   * Adds files to a Tar Archive
   * @param out
   * @param file
   * @param path
   */
  def addFilesToTar(out: TarArchiveOutputStream, file: File, path: Path): Unit = {
    out.putArchiveEntry(new TarArchiveEntry(file, path.resolve(file.getName).toString))

    if (file.isFile) {
      val in = new BufferedInputStream(new FileInputStream(file))
      io.copyBytes(in, out)
      in.close()
      out.closeArchiveEntry()
    } else {
      out.closeArchiveEntry()
      file.listFiles.toList.foreach(childFile => addFilesToTar(out, childFile, path.resolve(childFile.getName)))
    }
  }

  /**
   * Deletes a resource in the given space.
   *
   * @param uri
   * The spaces URI on which the operation should be excuted.
   * @param relativePath
   * The path relativ to the container.
   * @return
   * Unit
   */
  final def delete(uri: SpacesURI, relativePath: Option[Path] = None) = httpRequest(new HttpDelete(url(uri, relativePath))) {
    case (204, response) ⇒
    // Alles gut.
    case _ ⇒
    // TODO throw exception ...
  }

  /**
   * Creates the URL to be called.
   *
   * @param uri
   * The URI of the space/ container.
   * @param relativePath
   * The relative path of the resource.
   * @return
   * The whole URL.
   */
  private def url(uri: SpacesURI, relativePath: Option[Path]): String = {
    val result = s"${uri.url}" + (relativePath match {
      case Some(path) ⇒ "?relativePath=" + path.toString
      case _ ⇒ ""
    })

    trace("Send request to : " + result)

    result
  }

  def httpRequest(request: HttpRequestBase, headers: BasicHeader*)(handler: (Int, HttpResponse) ⇒ Unit) = {
    // Prepare Request
    request.setHeaders(headers.toArray)

    val client = HttpClients.createDefault()
    client.execute(request, new ResponseHandler[Unit] {
      override def handleResponse(response: HttpResponse) = {
        handler(response.getStatusLine.getStatusCode, response)
      }
    })
    client.close()
  }

}

/**
 * Simple resource to trigger Tests.
 */
final class SpacesTestClient

  extends StaticResource {

  Get {
    throw new Exception("HUHU")
    // Ich lasse mir ein TAR schicken und tue so als ob es vom Server gepackt wird.
    // SpacesClient.get(SpacesURI("myspace"), FileSystems.getDefault.getPath("/Users/michael/Downloads"))
    SpacesClient.put(SpacesURI("myspace"), FileSystems.getDefault.getPath("/Users/michael/Downloads"))
    "HelloWorld".getBytes

  }

}
