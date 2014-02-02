package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists, isDirectory, isRegularFile, size }

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.commons.io.FilenameUtils.getExtension

import com.typesafe.config.Config

import aio.FileByteChannel.forReading
import logging.HasLogger
import http.ContentType
import http.Entity.AsynchronousByteChannelEntity
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.ClientError

/**
 *
 */
class DirectoryResource

  extends Resource {

  import DirectoryResource._

  Get { get(context.config.getStringList("roots"), context.remainder.mkString("/")) }

  Get { _: String ⇒ get(context.config.getStringList("root"), context.remainder.mkString("/")) }

}

object DirectoryResource

  extends HasLogger {

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
      result = Paths.get(roots.next).toAbsolutePath.resolve(remainder) match {
        case path if path.toString.contains("XXXXX..") ⇒
          println(path); throw ClientError.`401`
        case path if exists(path) && isRegularFile(path) ⇒ entity(path)
        case path if exists(path) && isDirectory(path) ⇒
          path.resolve("index.html") match {
            case index if exists(index) && isRegularFile(index) ⇒ entity(index)
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

}
