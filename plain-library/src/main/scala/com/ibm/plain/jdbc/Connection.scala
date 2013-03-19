package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection }
import java.util.concurrent.BlockingDeque
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }

/**
 * A pooled java.sql.Connection.
 */
final class Connection private[jdbc] (

  connection: JdbcConnection,

  var idle: BlockingDeque[Connection])

  extends ConnectionWrapper(connection) {

  override final def toString = super.toString + " active=" + isActive

  override final def close: Unit = {
    if (!connection.isClosed) {
      connection.clearWarnings
      if (!idle.contains(this)) {
        deactivate
        idle.putFirst(this)
      } else throw new IllegalStateException("Connection already in idle list : " + this)
    } else doClose
  }

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

