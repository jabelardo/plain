package com.ibm.plain
package io

import scala.language.reflectiveCalls

/**
 *
 */
private[io] final class WithClosable[A <: { def close() }] private (

    closable: A) {

  private def call[B](block: A ⇒ B) = {
    var ex: Throwable = null
    try {
      block(closable)
    } catch {
      case e: Throwable ⇒
        ex = e
        throw e
    } finally {
      if (null != closable) {
        if (null != ex) {
          try {
            closable.close
          } catch {
            case e: Throwable ⇒ ex.addSuppressed(e)
          }
        } else {
          closable.close
        }
      }
    }
  }

}

/**
 *
 */
private[io] object WithClosable {

  final def apply[A <: { def close() }, B](closable: A)(block: A ⇒ B) = new WithClosable(closable).call(block)

}
