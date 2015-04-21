package com.ibm.plain
package integration
package spaces

import java.nio.file.{ Path, Paths, StandardCopyOption }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile, copy, move, delete, readAllBytes }

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
    trace(s"GET : $request")
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
   * Upload a complete directory file.
   */
  Put { entity: Entity ⇒
    trace(s"PUT : $request")
    entity match {
      case Entity(contenttype, length, _) ⇒
        val container = computePathToContainerFile(context)
        trace(s"PUT : container = $container")
        if (fexists(container)) {
          trace(s"PUT : container already exists and is deleted : $container")
          delete(container)
        }
        exchange.transferTo(
          FileConduit.forWriting(container),
          context ⇒ {
            trace(s"PUT : transfer completed, container size = ${container.toFile.length}")
            context.response ++ Success.`201`
          })
      case _ ⇒ throw ServerError.`501`
    }
    ()
  }

  /**
   * Receive a json of containers and files inside them and download them as one container file.
   */
  Post { entity: Entity ⇒
    trace(s"POST : $request")
    val input: JObject = entity match {
      case ArrayEntity(array, offset, length, _) ⇒
        try {
          val inputfilepath = Paths.get(new String(array, offset, length.toInt, text.`UTF-8`))
          trace(s"Trying to load input from temp file : $inputfilepath")
          val inputfile = new String(readAllBytes(inputfilepath), text.`UTF-8`)
          trace(s"Input read from file : $inputfile")
          Json.parse(inputfile).asObject
        } catch { case _: Throwable ⇒ throw ClientError.`400` }
      case e ⇒
        error(s"POST : Entity not handled : $e")
        throw ClientError.`413`
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
    checkMinimumFileSpace
    val root = context.config.getString("spaces-directory")
    val space = context.variables.getOrElse("space", null)
    val container = if (null != defaultcontainer) defaultcontainer else context.variables.getOrElse("container", defaultcontainer)
    require(null != space && null != container, throw ClientError.`400`)
    val containeruuid = try Uuid.fromString(container) catch { case e: Throwable ⇒ throw ClientError.`400` }
    computeDirectory(root, space).resolve(containeruuid + extension)
  }

  private final def extractFilesFromContainers(context: Context, input: JObject): Path = try {
    import SpacesClient.{ packDirectory, unpackDirectory }
    trace(s"extractFilesFromContainers : input = $input")
    checkMinimumFileSpace
    val collectdir = temporaryDirectory.toPath
    input.toList.foreach {
      case (container, files) ⇒
        val containerdir = temporaryDirectory.toPath
        val unpackdir = temporaryDirectory.toPath
        try {
          val containerfile = computePathToContainerFile(context, container)
          val lz4file = unpackdir.resolve("lz4")
          copy(containerfile, lz4file, StandardCopyOption.REPLACE_EXISTING)
          unpackDirectory(containerdir, lz4file.toFile, true) // ignore errors
        } catch {
          case e: Throwable ⇒
            warn(s"extractFilesFromContainers : ignored = $e")
        }
        val filelist = {
          val fileinput = files.asArray.map(_.asString).toList
          if (1 == fileinput.size && "*" == fileinput.apply(0)) {
            containerdir.toFile.list.toList.filter(f ⇒ f.endsWith("CATPart") || f.endsWith("CATProduct"))
          } else {
            fileinput
          }
        }
        trace(s"extractFilesFromContainers : input filelist = $filelist")
        filelist.foreach(f ⇒ {
          val from = containerdir.resolve(f)
          if (!fexists(from)) {
            val fromfallback = fallbackDirectory.resolve(f)
            if (!fexists(fromfallback)) {
              error(s"POST : File could not be extracted from repository and is also missing in the 'fallback' directory : filename = $f fallback directory = $fallbackDirectory")
            }
          }
        })
        filelist.foreach(f ⇒ {
          trace(s"Trying to collect file : $f")
          val from = containerdir.resolve(f)
          val to = collectdir.resolve(f)
          if (fexists(from)) {
            move(from, to, StandardCopyOption.REPLACE_EXISTING)
          } else {
            warn(s"POST : Could not extract a repository file : filename = ${from.getFileName} directory = ${from.getParent}")
            warn(s"POST : Looking for file in the spaces 'fallback' directory : filename = $f fallback directory = $fallbackDirectory")
            val fromfallback = fallbackDirectory.resolve(f)
            if (fexists(fromfallback)) {
              copy(fromfallback, to, StandardCopyOption.REPLACE_EXISTING)
              warn(s"POST : Copied file from the 'fallback' directory : filename = $f")
            } else {
              error(s"POST : File does not exist and is missing in the 'fallback' directory : filename = $f")
              illegalState(s"POST : File does not exist and is missing in the 'fallback' directory : filename = $f")
            }
          }
        })
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
