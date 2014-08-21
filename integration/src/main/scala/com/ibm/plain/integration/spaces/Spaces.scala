package com.ibm

package plain

package integration

package spaces

import java.nio.file.Files.{ exists, isDirectory, isRegularFile }
import java.nio.file.{ Path, Paths }

import org.apache.commons.io.FileUtils.deleteDirectory

import bootstrap.{ ExternalComponent, Singleton }
import logging.Logger

/**
 *
 */
final class Spaces

  extends ExternalComponent[Spaces](

    spaces.isEnabled,

    "plain-integration-spaces",

    classOf[infrastructure.Infrastructure])

  with Logger {

  override def start = {

    spaceslist.foreach { space ⇒
      def log(fun: String ⇒ Unit, message: String, space: Space, path: Path): Unit = fun("name: " + space.name + ", " + message + ": " + path.toAbsolutePath)
      val (fun, message, path): (String ⇒ Unit, String, Path) = rootDirectory.resolve(space.name) match {
        case path if !exists(path.getParent) ⇒
          (error, "invalid root-directory", path.getParent)
        case path if path.toString.contains("..") ⇒
          (error, "invalid directory", path)
        case path if exists(path) && isRegularFile(path) ⇒
          (error, "invalid directory (is a file)", path)
        case path if exists(path) && isDirectory(path) && space.purgeOnStartup ⇒
          deleteDirectory(path.toFile)
          io.createDirectory(path)
          (trace, "directory purged", path)
        case path if exists(path) && isDirectory(path) ⇒
          (trace, "directory exists", path)
        case path ⇒
          try {
            io.createDirectory(path)
            (trace, "directory created", path)
          } catch {
            case e: Throwable ⇒ (error, e.getMessage, path)
          }
      }
      if (space.purgeOnShutdown) io.deleteOnExit(rootDirectory.resolve(space.name).toFile)
      log(fun, message, space, path)
    }
    Spaces.instance(this)
    this
  }

  override def stop = {
    Spaces.resetInstance
    this
  }

}

/**
 *
 */
object Spaces

  extends Singleton[Spaces]
