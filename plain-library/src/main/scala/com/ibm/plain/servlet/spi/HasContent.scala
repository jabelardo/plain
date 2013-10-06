package com.ibm

package plain

package servlet

package spi

import java.io.{ BufferedReader, PrintWriter }
import java.nio.ByteBuffer

trait HasContent {

  self: HasContext ⇒

  override final def toString = { bytebuffer.flip; new String(bytebuffer.array, 0, bytebuffer.remaining) }

  final def getContentLength: Int = unsupported

  final def getContentType: String = unsupported

  final def getInputStream: ServletInputStream = unsupported

  final def getReader: BufferedReader = unsupported

  final def getOutputStream: ServletOutputStream = ServletOutputStream(new io.ByteBufferOutputStream(bytebuffer))

  final def getWriter: PrintWriter = unsupported

  final def getCharacterEncoding: String = unsupported

  final def setContentLength(length: Int) = { println("contentlength " + length) }

  final def setContentType(contenttype: String) = { println("contenttype " + contenttype) }

  final def setCharacterEncoding(encoding: String) = unsupported

  private[this] final val bytebuffer: ByteBuffer = ByteBuffer.allocate(10000)

}

