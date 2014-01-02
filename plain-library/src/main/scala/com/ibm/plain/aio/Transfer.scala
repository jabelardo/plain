package com.ibm

package plain

package aio

import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

/**
 *
 */
final case class Transfer(source: Channel, destination: Channel, encoder: Option[Encoder]) {

  val buffer = java.nio.ByteBuffer.allocateDirect(1024)

}

