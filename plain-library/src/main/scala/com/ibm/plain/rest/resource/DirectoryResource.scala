package com.ibm

package plain

package rest

package resource

import java.nio.file.{ Path, Paths }
import java.nio.file.Files.{ exists, isDirectory, isRegularFile, size }

import scala.reflect.runtime.universe

import org.apache.commons.io.FilenameUtils.getExtension

import aio.FileByteChannel.forReading
import http.ContentType
import http.Entity.AsynchronousByteChannelEntity
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.ClientError

/**
 *
 */
class DirectoryResource

  extends Resource {

  Get { get }

  Get { _: String ⇒ get }

  private[this] final def get = {

    def entity(file: Path) = {
      AsynchronousByteChannelEntity(
        forReading(file),
        ContentType(forExtension(getExtension(file.toString)).getOrElse(`application/octet-stream`)),
        size(file))
    }

    Paths.get(context.config.getString("root")).toAbsolutePath.resolve(context.remainder.mkString("/")) match {
      case file if file.toString.contains("..") ⇒ throw ClientError.`401`
      case file if exists(file) && isRegularFile(file) ⇒ entity(file)
      case file if exists(file) && isDirectory(file) ⇒
        file.resolve("index.html") match {
          case index if exists(index) && isRegularFile(index) ⇒ entity(index)
          case index ⇒ throw ClientError.`406`
        }
      case _ ⇒ throw ClientError.`404`
    }
  }

}
