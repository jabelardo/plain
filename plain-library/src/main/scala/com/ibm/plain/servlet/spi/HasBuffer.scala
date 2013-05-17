package com.ibm

package plain

package servlet

package spi

trait HasBuffer {

  final def flushBuffer = unsupported

  final def reset = unsupported

  final def resetBuffer = unsupported

  final def getBufferSize: Int = unsupported

  final def setBufferSize(size: Int) = unsupported

}

