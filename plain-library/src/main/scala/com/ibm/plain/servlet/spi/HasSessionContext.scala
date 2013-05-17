package com.ibm

package plain

package servlet

package spi

import javax.servlet.http.{ HttpSessionContext ⇒ JHttpSessionContext }

/**
 *
 */
trait HasSessionContext {

  @deprecated("2.1", "will be removed") final def getSessionContext: JHttpSessionContext = deprecated

}

