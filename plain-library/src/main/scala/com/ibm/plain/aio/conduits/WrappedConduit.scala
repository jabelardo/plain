package com.ibm

package plain

package aio

package conduits

import java.nio.ByteBuffer
import java.nio.channels.{ AsynchronousChannel ⇒ BaseChannel, CompletionHandler }

/**
 * Wrap something that is "just" an AsynchronousChannel to become an AsynchronousByteChannel.
 */
trait WrappedConduit

  extends Conduit[WrappedConduit] {

  override final def close = wrappedchannel.close

  override final def isOpen = wrappedchannel.isOpen

  protected[this] def wrappedchannel: BaseChannel

  protected[this] final val underlyingchannel: WrappedConduit = this

}