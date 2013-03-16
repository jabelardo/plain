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
import rest.{ Context, Resource }

/**
 *
 */
class JarResource

  extends Resource {

  Get {
    val path = context.remainder.mkString("/")
    getClass.getClassLoader.getResourceAsStream(root(context) + path) match {
      case in if null != in && -1 < { try in.available catch { case _: Throwable ⇒ -1 } } ⇒
        val out = new ByteArrayOutputStream(in.available)
        try {
          copy(in, out)
          ArrayEntity(out.toByteArray, ContentType(forExtension(getExtension(path)).getOrElse(`application/octet-stream`)))
        } finally {
          in.close
          out.close
        }
      case _ ⇒ JarResource.debug(root(context) + context.remainder.mkString("/") + " not found."); throw ClientError.`404`
    }

  }

  private[this] final def root(context: Context) = {
    var r = context.config.getString("root")
    if (!r.endsWith("/")) r = r + "/"
    if (r.startsWith("/")) r = r.drop(1)
    r
  }

}

/**
 *
 */
object JarResource extends HasLogger 

