package com.ibm

package plain

package rest

package resource

import java.nio.file.Files.{ exists, isRegularFile, size }
import java.nio.file.Paths

import com.ibm.plain.rest.{ Context, Resource }

import aio.FileByteChannel.forReading
import http.ContentType
import http.Entity.AsynchronousByteChannelEntity
import http.MimeType.`text/plain`
import http.Status.ClientError

/**
 *
 */
class DirectoryResource

  extends Resource {

  Get {
    root(context).resolve(context.remainder.mkString("/")) match {
      case file if file.toString.contains("..") ⇒ throw ClientError.`401`
      case file if exists(file) && isRegularFile(file) ⇒
        AsynchronousByteChannelEntity(forReading(file), ContentType(`text/plain`, text.`UTF-8`), size(file))
      case file if exists(file) ⇒ throw ClientError.`406`
      case f ⇒ throw ClientError.`404`
    }
  }

  private[this] final def root(context: Context) = Paths.get(context.config.getString("root")).toAbsolutePath

}

