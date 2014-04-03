package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ createDirectories, exists ⇒ fexists, isDirectory, isRegularFile, size }

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.io.filefilter.RegexFileFilter

import com.typesafe.config.Config

import aio.{ AsynchronousFileByteChannel, AsynchronousFixedLengthChannel }
import aio.AsynchronousFileByteChannel.{ forReading, forWriting }
import logging.Logger
import http.ContentType
import http.Entity
import http.Entity.{ AsynchronousByteChannelEntity, ArrayEntity, ContentEntity }
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.{ ClientError, ServerError, Success }

/**
 *
 */
class DirectoryResource

  extends Resource {

  import DirectoryResource._

  Get { get(context.config.getStringList("roots"), context.remainder.mkString("/")) }

  Get { _: String ⇒ get(context.config.getStringList("roots"), context.remainder.mkString("/")) }

  Delete { val path = context.remainder.mkString("/"); path }

  Delete { form: Form ⇒ val path = context.remainder.mkString("/"); path }

  /**
   * Creates a directory with the remainder as path relative to the first root. All intermediate directories are also created if necessary.
   */
  Put {
    put(context.config.getStringList("roots").head, context.remainder.mkString("/"))
    context.response ++ Success.`201`
    ""
  }

  Put { entity: Entity ⇒
    val filename = "/tmp/test/blabla.bin"
    println("PUT " + entity + " " + entity.length + " " + filename)
    entity match {
      case ArrayEntity(array, offset, length, contenttype) ⇒
      case Entity(contenttype, length, encodable) ⇒
        println(encodable + " " + length + " " + contenttype)
        AsynchronousByteChannelEntity(
          forWriting(filename),
          contenttype,
          length,
          contenttype.mimetype.encodable)
    }
    ""
  }

}

/**
 *
 */
object DirectoryResource

  extends Logger {

  final def get(list: Seq[String], remainder: String) = {
    val roots = list.iterator
    var found = false
    var result: AsynchronousByteChannelEntity = null

    def entity(path: Path) = {
      found = true
      val contenttype = ContentType(forExtension(getExtension(path.toString)).getOrElse(`application/octet-stream`))
      AsynchronousByteChannelEntity(
        forReading(path),
        contenttype,
        size(path),
        contenttype.mimetype.encodable)
    }

    while (!found && roots.hasNext) {
      val root = roots.next
      trace("root=" + Paths.get(root).toAbsolutePath + " file=" + remainder)
      result = Paths.get(root).toAbsolutePath.resolve(remainder) match {
        case path if path.toString.contains("..") ⇒ throw ClientError.`401`
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

  final def put(root: String, remainder: String): Unit = Paths.get(root).toAbsolutePath.resolve(remainder) match {
    case path if path.toString.contains("..") ⇒ throw ClientError.`401`
    case path if fexists(path) && isRegularFile(path) ⇒ throw ClientError.`409`
    case path if fexists(path) && isDirectory(path) ⇒
    case path ⇒ try { val x = createDirectories(path); println(x) } catch { case e: Throwable ⇒ throw ServerError.`503` }
  }

  private[this] final val welcomefiles = "([iI]ndex\\.((htm[l]*)|(jsp)))|([dD]efault\\.((htm[l]*)|(jsp)))"

  private[this] final val welcomefilter: java.io.FileFilter = new RegexFileFilter(welcomefiles)

}
