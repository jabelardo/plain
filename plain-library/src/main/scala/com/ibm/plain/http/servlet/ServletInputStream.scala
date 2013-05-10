package com.ibm

package plain

package http

package servlet

import java.io.InputStream
import javax.servlet.{ ServletInputStream ⇒ JServletInputStream }

/**
 *
 */
final class ServletInputStream(in: InputStream)

  extends JServletInputStream {

  @inline override final def read: Int = in.read

  @inline override final def read(a: Array[Byte]) = in.read(a, 0, a.length)

  @inline override final def read(a: Array[Byte], offset: Int, length: Int) = in.read(a, offset, length)

  @inline override final def available: Int = in.available

}

