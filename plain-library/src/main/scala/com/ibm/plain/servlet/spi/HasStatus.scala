package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasStatus {

  final def isCommitted: Boolean = unsupported

  final def setStatus(status: Int) = unsupported

  final def setStatus(status: Int, msg: String) = deprecated

}

