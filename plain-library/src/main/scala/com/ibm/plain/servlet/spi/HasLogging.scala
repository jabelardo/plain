package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasLogging {

  final def log(e: Exception, msg: String) = deprecated

  final def log(msg: String) = unsupported

  final def log(msg: String, e: Throwable) = unsupported

}

