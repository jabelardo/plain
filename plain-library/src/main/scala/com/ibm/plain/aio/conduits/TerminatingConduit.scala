package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

/**
 * A TerminatingConduit cannot be connected with other Conduits. It is either the only SourceConduit or the only SinkConduit in a transfer.
 */
trait TerminatingConduit

  extends TerminatingSourceConduit

  with TerminatingSinkConduit

/**
 * Used as the single SourceConduit as source for a transfer.
 */
trait TerminatingSourceConduit

  extends SourceConduit

/**
 * Used as the single SinkConduit as sink for a transfer.
 */
trait TerminatingSinkConduit

  extends SinkConduit

