package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection, PreparedStatement }
import java.util.concurrent.BlockingDeque
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }

import scala.collection.mutable.HashMap

/**
 * A pooled java.sql.Connection.
 */
final class Connection private[jdbc] (

  private[this] final val connection: JdbcConnection,

  private[this] final val idle: BlockingDeque[Connection])

    extends ConnectionWrapper(connection) {

  override final def toString = super.toString + " active=" + active.get

  override final def prepareStatement(sql: String, options: Array[Int]): PreparedStatement = statementcache.get(sql) match {
    case Some(stmt) ⇒
      stmt
    case _ ⇒
      val stmt = super.prepareStatement(sql, options)
      statementcache.put(sql, stmt)
      stmt
  }

  override final def close: Unit = {
    if (!connection.isClosed) {
      connection.clearWarnings
      if (!idle.contains(this)) {
        active.set(false)
        idle.putFirst(this)
      } else illegalState("Connection already in idle list : " + this)
    } else doClose
  }

  private[jdbc] final def doClose = {
    idle.remove(this)
    statementcache.values.foreach(stmt ⇒ ignore(stmt.close))
    super.close
  }

  private[jdbc] final val lastaccessed = new AtomicLong(time.now)

  private[jdbc] final val active = new AtomicBoolean(false)

  private[this] final val statementcache = new HashMap[String, PreparedStatement]

}

