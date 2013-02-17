package com.ibm

package plain

package rest

package resource

import java.nio.file.Files.{ exists, isRegularFile, size }
import java.nio.file.Paths

import com.typesafe.config.{ Config, ConfigFactory }

import config.CheckedConfig
import aio.FileByteChannel._
import http.{ Request, Response, ContentType }
import http.Status._
import http.Entity._
import http.MimeType.`text/plain`
import logging.HasLogger

/**
 *
 */
class DirectoryResource

  extends Resource {

  import DirectoryResource._

  val rootDirectory = Paths.get("/Users/guido/Development/Projects/plain")

  Get {
    rootDirectory.resolve(context.remainder.mkString("/")) match {
      case file if exists(file) && isRegularFile(file) ⇒ AsynchronousByteChannelEntity(forReading(file), ContentType(`text/plain`, text.`UTF-8`), size(file))
      case file if exists(file) ⇒ throw ClientError.`406`
      case _ ⇒ throw ClientError.`404`
    }

  }

}

/**
 *
 */
object DirectoryResource

  extends HasLogger {

  /**
   * A per-resource provided configuration.
   */
  final case class DirectoryResourceConfiguration(

    path: String)

    extends CheckedConfig {

    import DirectoryResourceConfiguration._

    override def handleError(e: Throwable) = {
      error(e.toString)
    }

    final val cfg: Config = config.settings.getConfig(path).withFallback(fallback)

    import cfg._

    final val displayName = getString("display-name")

    final val rootDirectory = Paths.get(getString("root-directory"))

    require(exists(rootDirectory), "directory-resource.root-directory does not exist : " + rootDirectory)

  }

  object DirectoryResourceConfiguration {

    final val fallback = ConfigFactory.parseString("""
        
    display-name = default
        
    root-directory = "/Users/guido/Development/Projects/plain"
        
    """)

  }

}

