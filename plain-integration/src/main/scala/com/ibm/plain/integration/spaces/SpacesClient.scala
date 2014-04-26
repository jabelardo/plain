package com.ibm

package plain

package integration

package spaces

import logging.Logger
import java.io.FileOutputStream
import java.nio.file.{FileSystems, FileSystem, Files, Path}
import java.util.UUID
import org.apache.http.impl.client.HttpClients
import org.apache.http.client.methods.HttpGet
import org.apache.http.{HttpResponse, HttpHeaders}
import org.apache.http.client.ResponseHandler
import com.ibm.plain.rest.StaticResource

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

  /**
   * Downloads a resource from the given URI.
   *
   * @param uri
   * The URI of the container which content should be downloaded.
   * @param file
   * The file where the download should be stored.
   * @param relativePath
   * The relative path of the resource within the container.
   * @param filename
   * Optional name of the file when it is stored on local file system. If None, the real name (served by the server) will be used.
   * @return
   * Unit
   */
  final def get(uri: SpacesURI, path: Path, relativePath: Option[Path] = None, fileName: Option[String] = None) = {
    require(Files.isDirectory(path), "The path must be an existing directory")

    trace(s"GET $uri")

    val client = HttpClients.createDefault
    val request = new HttpGet(url(uri, relativePath))
    request.setHeader(HttpHeaders.ACCEPT_ENCODING, downloadEncoding)

    val handler = new ResponseHandler[Unit] {
      def handleResponse(response: HttpResponse): Unit = {
        response.getStatusLine.getStatusCode match {
          case 200 =>
            val filePath = path.resolve(fileName match {
              case Some(name) => name
              case None => response.getHeaders("Content-Disposition").toList.headOption match {
                case Some(value) => value.getValue
                case None =>
                  // TODO: Throw exception - Muss von SpacesServer mit geliefert werden.
                  ""
              }
            })

            // TODO: TAR entpacken, wenn denn dann ein TAR von SpacesServer geliefert wird ...
            val out = new FileOutputStream(filePath.toFile)
            val in = response.getEntity.getContent

            io.copyBytesIo(in, out)
            out.close()
          case statusCode =>
            // TODO: Throw exception?
            error(s"Ach du Scheiße hier ist was schief gelaufen ... $statusCode")
        }
      }
    }

    client.execute(request, handler)
    client.close()
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
}

/**
 * Simple resource to trigger Tests.
 */
final class SpacesTestClient

  extends StaticResource {

  Get {

    SpacesClient.get(SpacesURI("fcb", "PBC2013.pdf"), FileSystems.getDefault.getPath("/Users/michael/Downloads/test.tar.gz"))
    "HelloWorld".getBytes

  }

}
