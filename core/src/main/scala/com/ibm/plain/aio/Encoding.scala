package com.ibm

package plain

package aio

import java.nio.ByteBuffer

/**
 *
 */
trait Encoding {

  def name: String

}

/**
 * Constructors from String.
 */
object Encoding {

  final def apply(name: String): Option[Encoding] = name match {
    case "chunked" ⇒ Some(new `chunked`)
    case "deflate" ⇒ Some(`deflate`)
    case "gzip"    ⇒ Some(`gzip`)
    case _         ⇒ None
  }

  /**
   *
   */
  abstract class NamedEncoding(val name: String) extends Encoding

  /**
   * Some commonly used encodings.
   */
  case object `identity` extends NamedEncoding("identity")

  class `chunked` extends NamedEncoding("chunked")

  case object `chunked` extends `chunked`

  case object `deflate` extends `chunked` { override final val name = "deflate" }

  case object `gzip` extends `chunked` { override final val name = "gzip" }

}
