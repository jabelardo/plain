package com.ibm

package plain

package servlet

package io

import java.io.OutputStream

import javax.{ servlet ⇒ js }

final class ServletOutputStream(

  private[this] final val out: OutputStream)

  extends js.ServletOutputStream {

  final def write(i: Int) = out.write(i)

  override final def write(a: Array[Byte]) = write(a, 0, a.length)

  override final def write(a: Array[Byte], offset: Int, length: Int) = out.write(a, offset, length)

  final def isReady = unsupported

  final def setWriteListener(listener: js.WriteListener) = unsupported

}