package com.ibm

package plain

package servlet

package spi

import javax.servlet.{ RequestDispatcher ⇒ JRequestDispatcher }

trait HasDispatcher {

  final def getRequestDispatcher(path: String): JRequestDispatcher = unsupported

  final def getNamedDispatcher(name: String): JRequestDispatcher = unsupported

}

