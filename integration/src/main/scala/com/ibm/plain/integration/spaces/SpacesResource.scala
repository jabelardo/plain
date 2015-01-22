package com.ibm.plain
package integration
package spaces

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile, copy, move }

import org.apache.commons.io.FileUtils.deleteDirectory

import aio.conduit.FileConduit
import aio.Exchange
import crypt.Uuid
import io.temporaryDirectory
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

    extends Resource

    with Logger {

  import SpacesResource._

  /**
   * Download an entire directory from the stored container file.
   */
  Get {
    val filepath = computePathToContainerFile(context)
    val length = filepath.toFile.length
    val source = FileConduit.forReading(filepath)
    trace(s"GET : source = $source, file = $filepath, length = $length")
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
        val container = computePathToContainerFile(context)
        trace(s"PUT : container = $container")
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
    trace(s"POST : $request")
    val input: JObject = entity match {
      case ArrayEntity(array, offset, length, _) ⇒
        try
          Json.parse(new String(array, offset, length.toInt, text.`UTF-8`)).asObject
        catch { case _: Throwable ⇒ throw ClientError.`400` }
      case _ ⇒ throw ClientError.`413`
    }
    val filepath = extractFilesFromContainers(context, input)
    val length = filepath.toFile.length
    val source = FileConduit.forReading(filepath)
    trace(s"POST : source = $source, file = $filepath, length = $length")
    exchange.transferFrom(source)
    ConduitEntity(
      source,
      ContentType(`application/gzip`),
      length,
      false)
  }

}

/**
 *
 */
object SpacesResource

    extends Logger {

  private final def computePathToContainerFile(context: Context, defaultcontainer: String = null): Path = {
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
    val container = if (null != defaultcontainer) defaultcontainer else context.variables.getOrElse("container", defaultcontainer)
    require(null != space && null != container, throw ClientError.`400`)
    val containeruuid = try Uuid.fromString(container) catch { case e: Throwable ⇒ throw ClientError.`400` }
    computeDirectory(root, space).resolve(containeruuid + extension)
  }

  private final def extractFilesFromContainers(context: Context, input: JObject): Path = try {
    import SpacesClient._
    val collectdir = temporaryDirectory.toPath
    input.toList.foreach {
      case (container, files) ⇒
        val containerfile = computePathToContainerFile(context, container)
        val containerdir = temporaryDirectory.toPath
        val unpackdir = temporaryDirectory.toPath
        val lz4file = unpackdir.resolve("lz4")
        copy(containerfile, lz4file)
        unpackDirectory(containerdir, lz4file)
        files.asArray.map(_.asString).foreach(f ⇒ { trace(s"Collect file : $f"); move(containerdir.resolve(f), collectdir.resolve(f)) })
        deleteDirectory(containerdir.toFile)
    }
    val lz4file = packDirectory(collectdir)
    deleteDirectory(collectdir.toFile)
    lz4file
  } catch {
    case e: Throwable ⇒
      error("extractFilesFromContainers failed : " + e)
      throw ClientError.`400`
  }

  private[this] final val extension = ".bin"

}
