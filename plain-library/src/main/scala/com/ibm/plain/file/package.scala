package com.ibm.plain

import java.nio.file.{Files, Path}
import scala.collection.JavaConversions._

/**
 * Created by michael on 27/04/14.
 *
 * Helper method for file handling.
 */
package object file {

  /**
   * This method deletes a file, even if it's an directory, even if it's not empty.
   *
   * @param path
   */
  def deleteIfExists(path: Path): Boolean = {
    if (Files.exists(path) && Files.isDirectory(path)) {
      // Delete its content and then itself.
      for (file <- Files.newDirectoryStream(path).iterator) {
        deleteIfExists(file)
      }

      Files.deleteIfExists(path)
    } else {
      Files.deleteIfExists(path)
    }
  }

}
