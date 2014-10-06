package com.ibm.plain
package integration
package spaces

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile }

import aio.conduit.FileConduit
import crypt.Uuid
import http.{ ContentType, Entity }
import http.Entity.{ ArrayEntity, ConduitEntity }
import http.MimeType._
import http.Status.{ ClientError, ServerError, Success }
import json.Json
import json.Json.JObject
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
    val filepath = computePathToContainerFile(context, extension)
    val length = filepath.toFile.length
    val source = FileConduit.forReading(filepath)
    exchange.transferFrom(source)
    ConduitEntity(
      source,
      ContentType(`application/gzip`),
      length,
      false)
  }

  /**
   * Upload a complete tar.gz file.
   */
  Put { entity: Entity ⇒
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

  /**
   * Receive a json of containers and files inside them and download them as a tar.gz file.
   */
  Post { entity: Entity ⇒
    debug(request)
    val input: JObject = entity match {
      case ArrayEntity(array, offset, length, _) ⇒
        try
          Json.parse(new String(array, offset, length.toInt, text.`UTF-8`)).asObject
        catch { case _: Throwable ⇒ throw ClientError.`400` }
      case _ ⇒ throw ClientError.`413`
    }
    error(input)
    extractFilesFromContainers(input)
    ()
  }

  private[this] final val extension = ".bin"

}

/**
 *
 */
object SpacesResource

    extends Logger {

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

  private final def extractFilesFromContainers(input: JObject): Path = {
    input.toList.foreach {
      case (container, files) ⇒
        error(container)
        files.asArray.foreach(error)
    }

    null
  }

}
