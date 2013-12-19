package com.ibm

package plain

package io

import java.{ io ⇒ io }
import java.util.Locale
import java.nio.charset.Charset

/**
 * Non-thread-safe version of java.io.PrintWriter.
 */
final class PrintWriter private (val outputstream: ByteArrayOutputStream)

  extends io.PrintWriter(PrintWriter.dummy) {

  final def setCharacterSet(cset: Charset) = characterset = cset

  override final def append(c: Char): io.PrintWriter = { print(c); this }

  override final def append(cs: CharSequence): io.PrintWriter = { write((if (null == cs) "null" else cs).toString); this }

  override final def append(cs: CharSequence, start: Int, end: Int): io.PrintWriter = { write((if (null == cs) "null" else cs.subSequence(start, end)).toString); this }

  override final def checkError: Boolean = error

  override final protected def clearError = error = false

  override final def close = try { outputstream.close } catch { case e: Throwable ⇒ error = true }

  override final def flush = try { outputstream.flush } catch { case e: Throwable ⇒ error = true }

  override final def format(locale: Locale, formats: String, args: Object*): io.PrintWriter = { write(String.format(locale, formats, args)); this }

  override final def format(formats: String, args: Object*): io.PrintWriter = { write(String.format(formats, args)); this }

  override final def print(b: Boolean) = write(b.toString)

  override final def print(c: Char) = write(c.toString)

  override final def print(s: Array[Char]) = write(new String(s))

  override final def print(d: Double) = write(d.toString)

  override final def print(f: Float) = write(f.toString)

  override final def print(i: Int) = write(i.toString)

  override final def print(l: Long) = write(l.toString)

  override final def print(o: Object) = write(o.toString)

  override final def print(s: String) = write(s)

  override final def printf(locale: Locale, formats: String, args: Object*): io.PrintWriter = format(locale, formats, args)

  override final def printf(formats: String, args: Object*): io.PrintWriter = format(formats, args)

  @inline override final def println = write("\n")

  override final def println(b: Boolean) = { print(b); this.println }

  override final def println(c: Char) = { print(c); this.println }

  override final def println(s: Array[Char]) = { print(s); this.println }

  override final def println(d: Double) = { print(d); this.println }

  override final def println(f: Float) = { print(f); this.println }

  override final def println(i: Int) = { print(i); this.println }

  override final def println(l: Long) = { print(l); this.println }

  override final def println(o: Object) = { print(o); this.println }

  override final def println(s: String) = { print(s); this.println }

  override final protected def setError = error = true

  override final def write(cs: Array[Char]) = write(cs, 0, cs.length)

  override final def write(cs: Array[Char], offset: Int, length: Int) = write(new String(cs, offset, length))

  override final def write(i: Int) = write(Character.toString(i.toChar))

  override final def write(s: String) = try { outputstream.write(s.getBytes(characterset)) } catch { case e: Throwable ⇒ error = true }

  override final def write(s: String, offset: Int, length: Int) = try { outputstream.write(s.substring(offset, offset + length).getBytes(characterset)) } catch { case e: Throwable ⇒ error = true }

  private[this] final var error = false

  private[this] final var characterset = text.`UTF-8`

}

object PrintWriter {

  final def apply(outputstream: ByteArrayOutputStream) = new PrintWriter(outputstream)

  private final val dummy = new io.PipedWriter

}