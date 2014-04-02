package com.ibm

package plain

package aio

import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel }

/**
 *
 */
final case class AsynchronousTransferTo(source: Channel, destination: Channel, encoder: Option[Encoder])

/**
 *
 */
final case class AsynchronousTransferFrom(source: Channel, destination: Channel, decoder: Any)
