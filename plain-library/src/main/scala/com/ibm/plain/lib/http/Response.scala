package com.ibm.plain

package lib

package http

import java.nio.CharBuffer

/**
 * A Renderable can put its content or fields into a CharBuffer.
 */
trait Renderable {

  def render(charbuffer: CharBuffer)

}

/**
 * The classic http response.
 */
case class Response(name: String)

