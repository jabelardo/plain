package com.ibm

package plain

package jdbc

import java.sql.{ Connection ⇒ JdbcConnection }

/**
 * A delegate to a java.sql.Connection.
 */
class ConnectionWrapper(connection: JdbcConnection)

  extends JdbcConnection {

  def close = connection.close
  final def clearWarnings = connection.clearWarnings
  final def commit = connection.commit
  final def createArrayOf(name: String, elements: Array[Object]) = connection.createArrayOf(name, elements)
  final def createBlob = connection.createBlob
  final def createClob = connection.createClob
  final def createNClob = connection.createNClob
  final def createSQLXML = connection.createSQLXML
  final def createStatement = connection.createStatement
  final def createStatement(resulttype: Int, concurrency: Int) = connection.createStatement(resulttype, concurrency)
  final def createStatement(resulttype: Int, concurrency: Int, hold: Int) = connection.createStatement(resulttype, concurrency, hold)
  final def createStruct(name: String, attributes: Array[Object]) = connection.createStruct(name, attributes)
  final def getAutoCommit = connection.getAutoCommit
  final def getCatalog = connection.getCatalog
  final def getClientInfo = connection.getClientInfo
  final def getClientInfo(name: String) = connection.getClientInfo(name)
  final def getHoldability = connection.getHoldability
  final def getMetaData = connection.getMetaData
  final def getTransactionIsolation = connection.getTransactionIsolation
  final def getTypeMap = connection.getTypeMap
  final def getWarnings = connection.getWarnings
  final def isClosed = connection.isClosed
  final def isReadOnly = connection.isReadOnly
  final def isValid(timeout: Int) = connection.isValid(timeout)
  final def nativeSQL(sql: String) = connection.nativeSQL(sql)
  final def prepareCall(sql: String) = connection.prepareCall(sql)
  final def prepareCall(sql: String, a: Int, b: Int) = connection.prepareCall(sql, a, b)
  final def prepareCall(sql: String, a: Int, b: Int, c: Int) = connection.prepareCall(sql, a, b, c)
  final def prepareStatement(sql: String) = connection.prepareStatement(sql)
  final def prepareStatement(sql: String, a: Int) = connection.prepareStatement(sql, a)
  final def prepareStatement(sql: String, a: Int, b: Int) = connection.prepareStatement(sql, a, b)
  final def prepareStatement(sql: String, a: Array[Int]) = connection.prepareStatement(sql, a)
  final def prepareStatement(sql: String, a: Int, b: Int, c: Int) = connection.prepareStatement(sql, a, b, c)
  final def prepareStatement(sql: String, a: Array[String]) = connection.prepareStatement(sql, a)
  final def releaseSavepoint(savepoint: java.sql.Savepoint) = connection.releaseSavepoint(savepoint)
  final def rollback = connection.rollback
  final def rollback(savepoint: java.sql.Savepoint) = connection.rollback(savepoint)
  final def setAutoCommit(value: Boolean) = connection.setAutoCommit(value)
  final def setCatalog(catalog: String) = connection.setCatalog(catalog)
  final def setClientInfo(properties: java.util.Properties) = connection.setClientInfo(properties)
  final def setClientInfo(name: String, value: String) = connection.setClientInfo(name, value)
  final def setHoldability(hold: Int) = connection.setHoldability(hold)
  final def setReadOnly(value: Boolean) = connection.setReadOnly(value)
  final def setSavepoint = connection.setSavepoint
  final def setSavepoint(name: String) = connection.setSavepoint(name)
  final def setTransactionIsolation(level: Int) = connection.setTransactionIsolation(level)
  final def setTypeMap(map: java.util.Map[String, Class[_]]) = connection.setTypeMap(map)
  final def isWrapperFor(c: Class[_]) = connection.isWrapperFor(c)
  final def unwrap[T](c: Class[T]) = connection.asInstanceOf[T]

  final def abort(executor: java.util.concurrent.Executor) = connection.abort(executor)
  final def getNetworkTimeout = connection.getNetworkTimeout
  final def setNetworkTimeout(executor: java.util.concurrent.Executor, timeout: Int) = connection.setNetworkTimeout(executor, timeout)
  final def getSchema = connection.getSchema
  final def setSchema(schema: String) = connection.setSchema(schema)

}