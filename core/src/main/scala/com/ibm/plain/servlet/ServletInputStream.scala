package com.ibm

package plain

package servlet

import java.io.InputStream

import javax.{ servlet ⇒ js }

final class ServletInputStream(

  private[this] final val in: InputStream)

    extends js.ServletInputStream {

  final def isFinished = !isReady

  final def isReady = 0 < in.available

  final def setReadListener(readlistener: js.ReadListener) = unsupported

  final def read = in.read

  override final def read(array: Array[Byte], offset: Int, length: Int) = in.read(array, offset, length)

  override final def read(array: Array[Byte]) = read(array, 0, array.length)

}