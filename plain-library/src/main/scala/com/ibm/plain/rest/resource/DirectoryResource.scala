package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists, isDirectory, isRegularFile, size }

import org.apache.commons.io.FilenameUtils.getExtension

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

  Get { get(context.config.getString("root"), context.remainder.mkString("/")) }

  Get { _: String ⇒ get(context.config.getString("root"), context.remainder.mkString("/")) }

}

object DirectoryResource

  extends HasLogger {

  final def get(root: String, remainder: String) = {

    def entity(file: Path) = {
      val contenttype = ContentType(forExtension(getExtension(file.toString)).getOrElse(`application/octet-stream`))
      AsynchronousByteChannelEntity(
        forReading(file),
        contenttype,
        size(file),
        contenttype.mimetype.encodable)
    }

    Paths.get(root).toAbsolutePath.resolve(remainder) match {
      case file if file.toString.contains("..") ⇒ throw ClientError.`401`
      case file if exists(file) && isRegularFile(file) ⇒ entity(file)
      case file if exists(file) && isDirectory(file) ⇒
        file.resolve("index.html") match {
          case index if exists(index) && isRegularFile(index) ⇒ entity(index)
          case index ⇒ throw ClientError.`406`
        }
      case p ⇒
        debug("404: " + p)
        throw ClientError.`404`
    }

  }

}
