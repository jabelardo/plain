package com.ibm

package plain

package integration

package spaces


import logging.Logger
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.UUID

/**
 *
 */
final class SpacesClient private

  extends Logger {

  //  def doGet(uri: String) = {
  //    val client = HttpClients.createDefault
  //    try {
  //      val request = new HttpGet("http://harryklein.munich.de.ibm.com:7070/spaces/blabla?blabla")
  //      request.setHeader(HttpHeaders.ACCEPT_ENCODING, "deflate")
  //
  //
  //      val handler = new ResponseHandler[String] {
  //        def handleResponse(response: HttpResponse): String = {
  //          response.getStatusLine.getStatusCode match {
  //            case 200 ⇒
  //              try {
  //                info(response.getEntity.getContentLength.toString)
  //                println("A")
  //                val f = new File("/Users/michael/Downloads/bla3.tar.gz")
  //                val out = new FileOutputStream(f)
  //
  //                info(f.getAbsolutePath)
  //
  //                println(response.getEntity)
  //                val in = response.getEntity.getContent
  //                println(in)
  //                println(io.copyBytesIo(in, out))
  //                out.close
  //                println("FERTSCH!")
  //              } catch {
  //                case e: Throwable => println(e)
  //              }
  //            case e ⇒ error("status code " + e)
  //          }
  //          null
  //        }
  //      }
  //      client.execute(request, handler)
  //    } finally client.close
  //  }

}

case class SpacesURI(node: String, container: String = UUID.randomUUID) {

  def uri = {
    s"spaces://$node/$container"
  }

}

/**
 *
 */
object SpacesClient {

  final def get(uri: SpacesURI, file: Path, relativePath: Option[Path] = None) = ???

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
   */
  final def delete(uri: SpacesURI, relativePath: Option[Path] = None): Boolean = ???

}