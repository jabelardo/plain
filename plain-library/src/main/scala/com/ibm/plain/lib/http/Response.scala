package com.ibm.plain

package lib

package http

import java.nio.ByteBuffer

import ResponseConstants._

/**
 * A Renderable can put its content or fields into a CharBuffer.
 */
trait Renderable {

  def render(implicit buffer: ByteBuffer)

}

/**
 * The classic http response.
 */
case class Response(

  version: Version,

  status: Status,

  more: String)

  extends Renderable {

  final def render(implicit buffer: ByteBuffer) = {
    version.render
    buffer.put(` `)
    status.render
    buffer.put(`\r\n`)
  }

}

