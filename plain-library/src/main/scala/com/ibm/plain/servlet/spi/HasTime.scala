package com.ibm

package plain

package servlet

package spi

/**
 *
 */
trait HasTime {

  final def getCreationTime: Long = unsupported

  final def getLastAccessedTime: Long = unsupported

  final def getMaxInactiveInterval: Int = unsupported

  final def setMaxInactiveInterval(interval: Int) = unsupported

  final def isNew: Boolean = unsupported

}

