package com.ibm

package plain

package integration

package spaces

import logging.Logger
import java.io.FileOutputStream
import java.nio.file.{FileSystems, FileSystem, Files, Path}
import java.util.UUID
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.{HttpRequestBase, HttpGet}
import org.apache.http.{HttpResponse, HttpHeaders}
import org.apache.http.client.ResponseHandler
import com.ibm.plain.rest.StaticResource
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.archivers.tar.{TarArchiveInputStream, TarArchiveOutputStream}
import org.apache.http.message.BasicHeader

/**
 *
 */
final class SpacesClient private

  extends Logger {

}

case class SpacesURI(node: String, container: String = UUID.randomUUID.toString) {

  /** Returns the url of the server where the spaces server component is running (without trailing '/') */
  private lazy val hostSpacesURL = {
    // TODO: Get the real server name via infrastructure component
    "localhost:7070/spaces"
  }

  /** Returns the unified resource name */
  lazy val urn = {
    s"spaces://$node/$container"
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
    case (200, response) =>
      require(Files.isDirectory(path), "The path must be an existing directory.")

      // Figure out the target directory
      val localFilePath = path.resolve(fileName match {
        case Some(name) => name
        case None => response.getHeaders("Content-Disposition").toList.headOption match {
          case Some(value) => value.getValue
          case None =>
            // TODO: Throw exception - Muss von SpacesServer mit geliefert werden.
            "undefined"
        }
      })

      // Clean & Create local directory
      file.deleteIfExists(localFilePath)
      Files.createDirectory(localFilePath)

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
    case (code, response) =>
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
  final def put(uri: SpacesURI, file: Path, relativePath: Option[Path] = None) = ???

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
  final def delete(uri: SpacesURI, relativePath: Option[Path] = None) = ???

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
    s"${uri.url}" + (relativePath match {
      case Some(path) => "?relativePath=" + path.toString
      case _ => ""
    })
  }

  def httpRequest(request: HttpRequestBase, headers: BasicHeader *)(handler: (Int, HttpResponse) => Unit) = {
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

    // Ich lasse mir ein TAR schicken und tue so als ob es vom Server gepackt wird.
    SpacesClient.get(SpacesURI("fcb", "1234-5678.tar"), FileSystems.getDefault.getPath("/Users/michael/Downloads"))
    "HelloWorld".getBytes

  }

}
