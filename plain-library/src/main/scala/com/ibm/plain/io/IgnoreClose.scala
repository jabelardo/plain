package com.ibm

package plain

package io

import java.io.Closeable

/**
 * If mixed in with a Closeable calls to close will be a NOOP, call doClose instead at the right spot.
 */
trait IgnoreClose

  extends Closeable {

  abstract override final def close = ()

  final def doClose = super.close

}
