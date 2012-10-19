package com.ibm.plain

package lib

package http

import java.nio.CharBuffer

import ResponseConstants._

/**
 * A Renderable can put its content or fields into a CharBuffer.
 */
trait Renderable {

  def render(implicit buffer: CharBuffer)

}

/**
 * The classic http response.
 */
case class Response(

  version: Version,

  status: Status,

  more: String)

  extends Renderable {

  final def render(implicit buffer: CharBuffer) = {
    version.render
    buffer.put(` `)
    status.render
    buffer.put(`\r\n`)
  }

}

