package com.ibm

package plain

package integration

package spaces

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ createDirectories, exists ⇒ fexists, isDirectory, isRegularFile, size ⇒ fsize, write ⇒ fwrite, readAllBytes, delete ⇒ fdelete }

import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.io.filefilter.RegexFileFilter
import org.apache.commons.io.FileUtils.deleteDirectory

import _root_.com.typesafe.config.Config

import scala.collection.JavaConversions.asScalaBuffer
import scala.language.implicitConversions
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe.TypeTag.Unit

import aio.{ AsynchronousFileByteChannel, AsynchronousFixedLengthChannel, Encoder, Exchange }
import aio.AsynchronousFileByteChannel.{ forReading, forWriting }
import concurrent.ioexecutor
import logging.Logger
import http.ContentType
import http.Entity
import http.Entity.{ AsynchronousByteChannelEntity, ArrayEntity, ContentEntity }
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.{ ClientError, ServerError, Success }
import rest._

/**
 *
 */
final class SpacesServer

  extends StaticResource {

  import SpacesServer._

  /**
   * Download a file or an entire directory as a zip file.
   */
  Get { get(context.config.getStringList("roots"), context.remainder.mkString("/"), exchange) }

  Get { data: Form ⇒ get(context.config.getStringList("roots"), context.remainder.mkString("/"), exchange) }

  /**
   * Delete a file or a directory.
   */
  Delete { response ++ delete(context.config.getStringList("roots").head, context.remainder.mkString("/")); () }

  Delete { _: String ⇒ response ++ delete(context.config.getStringList("roots").head, context.remainder.mkString("/")); () }

  /**
   * Creates a directory with the remainder as path relative to the first root. All intermediate directories are also created if necessary.
   */
  Put { response ++ put(context.config.getStringList("roots").head, context.remainder.mkString("/")); () }

  /**
   * Upload a file.
   */
  Put { entity: Entity ⇒
    val root = context.config.getStringList("roots").head
    val path = context.remainder.mkString("/")
    entity match {
      case ArrayEntity(array, offset, length, contenttype) ⇒
        response ++ put(check(root, path), array, offset, length, contenttype); ()
      case e @ Entity(contenttype, length, encodable) if 0 < length ⇒
        exchange.transferTo(forWriting(check(root, path), length), length, context ⇒ {
          context.response ++ Success.`201`
        })
      case e ⇒ throw ServerError.`501` // needs transfer decoding
    }
  }

  /**
   * What's left for Post?
   */

}

/**
 *
 */
object SpacesServer

  extends Logger {

  final def get(list: Seq[String], remainder: String, exchange: Exchange[Context]) = {
    val roots = list.iterator
    var found = false
    var result: Entity = null

    @inline def entity(path: Path): Entity = {
      found = true
      val length = fsize(path)
      val contenttype = ContentType(forExtension(getExtension(path.toString)).getOrElse(`application/octet-stream`))
      if (length <= exchange.available) {
        ArrayEntity(readAllBytes(path), contenttype)
      } else {
        val source = forReading(path)
        exchange.transferFrom(source)
        AsynchronousByteChannelEntity(
          source,
          contenttype,
          length,
          contenttype.mimetype.encodable)
      }
    }
    while (!found && roots.hasNext) {
      val root = roots.next
      trace("root=" + Paths.get(root).toAbsolutePath + " file=" + remainder)
      result = Paths.get(root).toAbsolutePath.resolve(remainder) match {
        case path if path.toString.contains("..") ⇒ throw ClientError.`406`
        case path if fexists(path) && isRegularFile(path) ⇒ entity(path)
        case path if fexists(path) && isDirectory(path) ⇒
          path.toFile.listFiles(welcomefilter).filter(f ⇒ f.exists && f.isFile).headOption match {
            case Some(file) ⇒
              trace("Matched welcomefile : " + file)
              entity(file.toPath)
            case _ ⇒ throw ClientError.`406`
          }
        case _ ⇒ null
      }
    }
    if (!found) {
      debug("404: " + remainder + "; " + roots.mkString(", "))
      throw ClientError.`404`
    } else {
      result
    }
  }

  final def exists(config: Config, remainder: List[String]): Boolean = {
    val roots = config.getStringList("roots").iterator
    var found = false
    while (!found && roots.hasNext) {
      Paths.get(roots.next).toAbsolutePath.resolve(remainder.mkString("/")) match {
        case path if path.toString.contains("..") ⇒ false
        case path if fexists(path) && isRegularFile(path) ⇒ found = true
        case path if fexists(path) && isDirectory(path) ⇒
          path.toFile.listFiles(welcomefilter).filter(f ⇒ f.exists && f.isFile).headOption match {
            case Some(index) ⇒ found = true
            case _ ⇒ false
          }
        case _ ⇒ null
      }
    }
    found
  }

  private final def delete(root: String, remainder: String): Success = {
    Paths.get(root).toAbsolutePath.resolve(remainder) match {
      case path if path.toString.contains("..") ⇒ throw ClientError.`406`
      case path if fexists(path) && isDirectory(path) ⇒ try {
        deleteDirectory(path.toFile)
        Success.`204`
      } catch { case e: Throwable ⇒ throw ServerError.`500` }
      case path if fexists(path) && isRegularFile(path) ⇒ try {
        fdelete(path)
        Success.`204`
      } catch { case e: Throwable ⇒ throw ServerError.`500` }
      case _ ⇒ throw ClientError.`404`
    }
  }

  /**
   * Put a file synchronously.
   */
  private final def put(path: Path, array: Array[Byte], offset: Int, length: Long, contenttype: ContentType): Success = try {
    fwrite(path, array)
    Success.`201`
  } catch { case e: Throwable ⇒ throw ServerError.`500` }

  /**
   * Check paths.
   */
  private final def check(root: String, remainder: String): Path = {
    Paths.get(root).toAbsolutePath.resolve(remainder) match {
      case path if path.toString.contains("..") ⇒ throw ClientError.`406`
      case path if fexists(path) && isDirectory(path) ⇒ throw ClientError.`409`
      case path ⇒ try {
        if (!fexists(path.getParent)) createDirectories(path.getParent)
        path
      } catch { case e: Throwable ⇒ throw ServerError.`500` }
    }

  }

  /**
   * Create directories.
   */
  private final def put(root: String, remainder: String): Success = {
    Paths.get(root).toAbsolutePath.resolve(remainder) match {
      case path if path.toString.contains("..") ⇒ throw ClientError.`406`
      case path if fexists(path) && isRegularFile(path) ⇒ throw ClientError.`409`
      case path if fexists(path) && isDirectory(path) ⇒ Success.`201`
      case path ⇒ try { createDirectories(path); Success.`201` } catch { case _: Throwable ⇒ throw ServerError.`500` }
    }
  }

  private[this] final val welcomefiles = "([iI]ndex\\.((htm[l]*)|(jsp)))|([dD]efault\\.((htm[l]*)|(jsp)))"

  private[this] final val welcomefilter: java.io.FileFilter = new RegexFileFilter(welcomefiles)

}
