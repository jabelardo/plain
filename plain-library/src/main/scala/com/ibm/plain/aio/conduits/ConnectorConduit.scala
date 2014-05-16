package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousByteChannel ⇒ Channel, CompletionHandler }

/**
 * A ConnectorConduit supports read and write operations on an underlying Channel.
 */
trait ConnectorConduit[C <: Channel]

  extends ConnectorSourceConduit[C]

  with ConnectorSinkConduit[C]

/**
 *
 */
trait ConnectorSourceConduit[C <: Channel]

  extends ConnectorConduitBase[C]

  with SourceConduit

/**
 *
 */
trait ConnectorSinkConduit[C <: Channel]

  extends ConnectorConduitBase[C]

  with SinkConduit

/**
 *
 */
trait ConnectorConduitBase[C <: Channel]

  extends Conduit {

  def close = underlyingchannel.close

  def isOpen = underlyingchannel.isOpen

  protected[this] def underlyingchannel: C

}

