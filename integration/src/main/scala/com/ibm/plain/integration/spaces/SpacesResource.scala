package com.ibm.plain
package integration
package spaces

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile }

import aio.conduit.{ ChunkedConduit, FileConduit, GzipConduit, TarConduit }
import crypt.Uuid
import http.{ ContentType, Entity }
import http.Entity.ConduitEntity
import http.MimeType.`application/gzip`
import http.Status.{ ClientError, ServerError, Success }
import logging.Logger
import rest.{ Context, Resource }

/**
 *
 */
final class SpacesResource

    extends Resource {

  import SpacesResource._

  /**
   * Download an entire directory from the stored container file.
   */
  Get {
    debug(request)
    val contenttype = ContentType(`application/gzip`)
    val path = computePathToContainerFile(context, extension)
    val length = path.toFile.length
    val source = FileConduit.forReading(path)
    exchange.transferFrom(source)
    ConduitEntity(
      source,
      contenttype,
      length,
      false).asInstanceOf[Entity]
  }

  /**
   * Upload a complete tar.gz file.
   */
  Put { entity: Entity ⇒
    debug(request)
    entity match {
      case Entity(contenttype, length, _) ⇒
        val container = computePathToContainerFile(context, extension)
        exchange.transferTo(
          FileConduit.forWriting(container),
          context ⇒ { context.response ++ Success.`201` })
      case _ ⇒ throw ServerError.`501`
    }
    ()
  }

  private[this] final val extension = ".bin"

}

/**
 *
 */
object SpacesResource {

  private final def computePathToContainerFile(context: Context, extension: String): Path = {
    def computeDirectory(root: String, directory: String): Path = {
      Paths.get(root).toAbsolutePath.resolve(directory) match {
        case path if path.toString.contains("..") ⇒ throw ClientError.`406`
        case path if fexists(path) && isRegularFile(path) ⇒ throw ClientError.`409`
        case path if fexists(path) && isDirectory(path) ⇒ path
        case path ⇒ io.createDirectory(path)
      }
    }
    val root = context.config.getString("spaces-directory")
    val space = context.variables.getOrElse("space", null)
    val container = context.variables.getOrElse("container", null)
    require(null != space && null != container, throw ClientError.`400`)
    val containeruuid = try Uuid.fromString(container) catch { case e: Throwable ⇒ throw ClientError.`400` }
    computeDirectory(root, space).resolve(containeruuid + extension)
  }

}
