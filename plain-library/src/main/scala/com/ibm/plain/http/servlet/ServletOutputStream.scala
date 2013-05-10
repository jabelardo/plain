package com.ibm

package plain

package http

package servlet

import java.io.OutputStream
import javax.servlet.{ ServletOutputStream ⇒ JServletOutputStream }

/**
 *
 */
final class ServletOutputStream(out: OutputStream)

  extends JServletOutputStream {

  @inline override final def flush = out.flush

  @inline override final def write(i: Int) = out.write(i)

  @inline override final def write(a: Array[Byte]) = out.write(a, 0, a.length)

  @inline override final def write(a: Array[Byte], offset: Int, length: Int) = out.write(a, offset, length)

}

