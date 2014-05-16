package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousChannel ⇒ BaseChannel, CompletionHandler }

/**
 * Wrap something that is "just" an AsynchronousChannel to become an AsynchronousByteChannel.
 */
trait WrapperConduit

  extends Conduit {

  override final def close = wrappedchannel.close

  override final def isOpen = wrappedchannel.isOpen

  protected[this] def wrappedchannel: BaseChannel

}