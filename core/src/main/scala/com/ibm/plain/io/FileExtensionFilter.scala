package com.ibm

package plain

package io

import java.io.{ File, FileFilter }

final case class FileExtensionFilter(

  extension: String)

    extends FileFilter {

  final def accept(file: File) = file.isFile && file.getName.toLowerCase.endsWith("." + extension)

}
