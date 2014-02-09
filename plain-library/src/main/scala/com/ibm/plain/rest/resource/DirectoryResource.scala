package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists ⇒ fexists, isDirectory, isRegularFile, size }

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.commons.io.FilenameUtils.getExtension

import com.typesafe.config.Config

import aio.FileByteChannel.forReading
import logging.HasLogger
import http.ContentType
import http.Entity.AsynchronousByteChannelEntity
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

}

object DirectoryResource

  extends HasLogger {

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
      result = Paths.get(roots.next).toAbsolutePath.resolve(remainder) match {
        case path if path.toString.contains("..") ⇒ throw ClientError.`401`
        case path if fexists(path) && isRegularFile(path) ⇒ entity(path)
        case path if fexists(path) && isDirectory(path) ⇒
          path.resolve("index.html") match {
            case index if fexists(index) && isRegularFile(index) ⇒ entity(index)
            case index ⇒ throw ClientError.`406`
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
          path.resolve("index.html") match {
            case index if fexists(index) && isRegularFile(index) ⇒ found = true
            case index ⇒ false
          }
        case _ ⇒ null
      }
    }

    found
  }

}
