package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection }
import java.util.concurrent.BlockingQueue
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }

/**
 * A pooled java.sql.Connection.
 */
final class Connection private[jdbc] (

  connection: JdbcConnection,

  var idle: BlockingQueue[Connection])

  extends ConnectionWrapper(connection) {

  override def toString = super.toString + " " + isActive

  @inline final def isActive = active.get

  @inline final def activate = active.set(true)

  @inline final def deactivate = active.set(false)

  private[jdbc] final def doClose = {
    idle = null
    super.close
  }

  private[jdbc] final val lastaccessed = new AtomicLong(time.now)

  private[this] final val active = new AtomicBoolean(false)

}

