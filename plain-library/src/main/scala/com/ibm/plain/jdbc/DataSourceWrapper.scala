package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection }
import javax.sql.{ DataSource ⇒ JdbcDataSource }

/**
 * A delegate to a javax.sql.DataSource.
 */
class DataSourceWrapper(

  datasource: JdbcDataSource,

  connectionfactory: ConnectionFactory)

  extends JdbcDataSource {

  final def getConnection: JdbcConnection = connectionfactory.newConnection

  final def getConnection(user: String, password: String) = getConnection

  final def getLoginTimeout = datasource.getLoginTimeout

  final def getLogWriter = datasource.getLogWriter

  final def getParentLogger = datasource.getParentLogger

  final def setLogWriter(writer: java.io.PrintWriter) = datasource.setLogWriter(writer)

  final def setLoginTimeout(seconds: Int) = datasource.setLoginTimeout(seconds)

  final def isWrapperFor(c: Class[_]) = datasource.isWrapperFor(c)

  final def unwrap[T](c: Class[T]) = datasource.asInstanceOf[T]

}

