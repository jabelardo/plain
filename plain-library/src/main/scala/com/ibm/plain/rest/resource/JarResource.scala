package com.ibm

package plain

package rest

package resource

import java.io.ByteArrayOutputStream

import org.apache.commons.io.FilenameUtils.getExtension

import http.ContentType
import http.Entity.ArrayEntity
import http.MimeType.{ `application/octet-stream`, forExtension }
import http.Status.ClientError
import io.{ copyBytesIo ⇒ copy }
import logging.HasLogger

/**
 *
 */
class JarResource

  extends Resource {

  Get {
    val path = resourcePath(context)
    getClass.getClassLoader.getResourceAsStream(path) match {
      case in if null != in && -1 < { try in.available catch { case _: Throwable ⇒ -1 } } ⇒
        val out = new ByteArrayOutputStream(in.available)
        try {
          copy(in, out)
          ArrayEntity(out.toByteArray, ContentType(forExtension(getExtension(path)).getOrElse(`application/octet-stream`)))
        } finally {
          in.close
          out.close
        }
      case _ ⇒ JarResource.debug(path + " not found."); throw ClientError.`404`
    }

  }

  @inline private[this] final def resourcePath(context: Context) = {
    var root = context.config.getString("root")
    if (!root.endsWith("/")) root = root + "/"
    if (root.startsWith("/")) root = root.drop(1)
    root + (if (context.remainder.isEmpty) "index.html" else context.remainder.mkString("/"))
  }

}

/**
 *
 */
object JarResource extends HasLogger

