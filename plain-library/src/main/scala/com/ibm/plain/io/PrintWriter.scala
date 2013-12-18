package com.ibm

package plain

package io

import java.{ io ⇒ io }
import java.util.Locale

/**
 * Non-thread-safe version of java.io.PrintWriter.
 */
final class PrintWriter private (private[this] final val outputstream: ByteArrayOutputStream)

  extends io.PrintWriter(PrintWriter.dummy) {

  final def getOutputStream = outputstream

  override final def append(c: Char): io.PrintWriter = unsupported

  override final def append(cs: CharSequence): io.PrintWriter = unsupported

  override final def append(cs: CharSequence, start: Int, end: Int): io.PrintWriter = unsupported

  override final def checkError: Boolean = unsupported

  override final protected def clearError = unsupported

  override final def close = unsupported

  override final def flush = unsupported

  override final def format(locale: Locale, formats: String, args: Object*): io.PrintWriter = unsupported

  override final def format(formats: String, args: Object*): io.PrintWriter = unsupported

  override final def print(b: Boolean) = unsupported

  override final def print(c: Char) = unsupported

  override final def print(s: Array[Char]) = unsupported

  override final def print(d: Double) = unsupported

  override final def print(f: Float) = unsupported

  override final def print(i: Int) = unsupported

  override final def print(l: Long) = unsupported

  override final def print(o: Object) = unsupported

  override final def print(s: String) = unsupported

  override final def printf(locale: Locale, formats: String, args: Object*): io.PrintWriter = format(locale, formats, args)

  override final def printf(formats: String, args: Object*): io.PrintWriter = format(formats, args)

  override final def println = unsupported

  override final def println(b: Boolean) = unsupported

  override final def println(c: Char) = unsupported

  override final def println(s: Array[Char]) = unsupported

  override final def println(d: Double) = unsupported

  override final def println(f: Float) = unsupported

  override final def println(i: Int) = unsupported

  override final def println(l: Long) = unsupported

  override final def println(o: Object) = unsupported

  override final def println(s: String) = unsupported

  override final protected def setError = unsupported

  override final def write(cs: Array[Char]) = unsupported

  override final def write(cs: Array[Char], offset: Int, length: Int) = unsupported

  override final def write(i: Int) = unsupported

  override final def write(s: String) = outputstream.write(s.getBytes("UTF-8"))

  override final def write(s: String, offset: Int, length: Int) = unsupported

}

object PrintWriter {

  final def apply(outputstream: ByteArrayOutputStream) = new PrintWriter(outputstream)

  private final val dummy = new io.PipedWriter

}