package com.ibm

package plain

package io

import java.io.Closeable

/**
 * When mixed in with a Closeable, calls to close will be a NOOP, call doClose instead at the right spot.
 */
trait IgnoringCloseable

  extends Closeable {

  abstract override def close = ()

  def doClose = super.close

}
