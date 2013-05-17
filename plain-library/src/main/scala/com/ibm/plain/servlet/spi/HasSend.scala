package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasSend {

  final def sendError(status: Int) = unsupported

  final def sendError(status: Int, msg: String) = unsupported

  final def sendRedirect(location: String) = unsupported

}

