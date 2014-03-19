package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile, size }
import java.nio.channels.{ CompletionHandler ⇒ Handler }

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.io.filefilter.RegexFileFilter

import com.typesafe.config.Config

import aio.{ AsynchronousFileByteChannel, AsynchronousFixedLengthChannel }
import aio.AsynchronousFileByteChannel.{ forReading, forWriting }
import logging.Logger
import http.ContentType
import http.Entity
import http.Entity.{ AsynchronousByteChannelEntity, ArrayEntity }
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.{ ClientError, ServerError }

/**
 *
 */
class DirectoryResource

  extends Resource {

  import DirectoryResource._

  Get { get(context.config.getStringList("roots"), context.remainder.mkString("/")) }

  Get { _: String ⇒ get(context.config.getStringList("roots"), context.remainder.mkString("/")) }

  Delete { val path = context.remainder.mkString("/"); println(path); println(context.request.query); path }

  Delete { form: Form ⇒ val path = context.remainder.mkString("/"); println(form); println(path); println(context.request.query); path }

  Put { entity: Entity ⇒
    val filename = "/tmp/test/blabla.bin"
    println("PUT" + entity + " " + entity.length + " " + filename)
    AsynchronousFileByteChannel.forWriting(filename).transferFrom(
      AsynchronousFixedLengthChannel(context.io.channel, 0, entity.length),
      context.io.writebuffer,
      filename,
      new Handler[Integer, String] {
        def completed(processed: Integer, filename: String) = "Thank you, " + filename
        def failed(e: Throwable, filename: String) = "Sorry, failed " + filename + " : " + e
      })
      Thread.sleep(20000)
      "Thank you!"
  }

}

object DirectoryResource

  extends Logger {

  def get(list: Seq[String], remainder: String) = {
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

  def exists(config: Config, remainder: List[String]): Boolean = {
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

  private[this] final val welcomefiles = "([iI]ndex\\.((htm[l]*)|(jsp)))|([dD]efault\\.((htm[l]*)|(jsp)))"

  private[this] final val welcomefilter: java.io.FileFilter = new RegexFileFilter(welcomefiles)

}
