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
  def clearWarnings = connection.clearWarnings
  def commit = connection.commit
  def createArrayOf(name: String, elements: Array[Object]) = connection.createArrayOf(name, elements)
  def createBlob = connection.createBlob
  def createClob = connection.createClob
  def createNClob = connection.createNClob
  def createSQLXML = connection.createSQLXML
  def createStatement = connection.createStatement
  def createStatement(resulttype: Int, concurrency: Int) = connection.createStatement(resulttype, concurrency)
  def createStatement(resulttype: Int, concurrency: Int, hold: Int) = connection.createStatement(resulttype, concurrency, hold)
  def createStruct(name: String, attributes: Array[Object]) = connection.createStruct(name, attributes)
  def getAutoCommit = connection.getAutoCommit
  def getCatalog = connection.getCatalog
  def getClientInfo = connection.getClientInfo
  def getClientInfo(name: String) = connection.getClientInfo(name)
  def getHoldability = connection.getHoldability
  def getMetaData = connection.getMetaData
  def getTransactionIsolation = connection.getTransactionIsolation
  def getTypeMap = connection.getTypeMap
  def getWarnings = connection.getWarnings
  def isClosed = connection.isClosed
  def isReadOnly = connection.isReadOnly
  def isValid(timeout: Int) = connection.isValid(timeout)
  def nativeSQL(sql: String) = connection.nativeSQL(sql)
  def prepareCall(sql: String) = connection.prepareCall(sql)
  def prepareCall(sql: String, a: Int, b: Int) = connection.prepareCall(sql, a, b)
  def prepareCall(sql: String, a: Int, b: Int, c: Int) = connection.prepareCall(sql, a, b, c)
  def prepareStatement(sql: String) = connection.prepareStatement(sql)
  def prepareStatement(sql: String, a: Int) = connection.prepareStatement(sql, a)
  def prepareStatement(sql: String, a: Int, b: Int) = connection.prepareStatement(sql, a, b)
  def prepareStatement(sql: String, a: Array[Int]) = connection.prepareStatement(sql, a)
  def prepareStatement(sql: String, a: Int, b: Int, c: Int) = connection.prepareStatement(sql, a, b, c)
  def prepareStatement(sql: String, a: Array[String]) = connection.prepareStatement(sql, a)
  def releaseSavepoint(savepoint: java.sql.Savepoint) = connection.releaseSavepoint(savepoint)
  def rollback = connection.rollback
  def rollback(savepoint: java.sql.Savepoint) = connection.rollback(savepoint)
  def setAutoCommit(value: Boolean) = connection.setAutoCommit(value)
  def setCatalog(catalog: String) = connection.setCatalog(catalog)
  def setClientInfo(properties: java.util.Properties) = connection.setClientInfo(properties)
  def setClientInfo(name: String, value: String) = connection.setClientInfo(name, value)
  def setHoldability(hold: Int) = connection.setHoldability(hold)
  def setReadOnly(value: Boolean) = connection.setReadOnly(value)
  def setSavepoint = connection.setSavepoint
  def setSavepoint(name: String) = connection.setSavepoint(name)
  def setTransactionIsolation(level: Int) = connection.setTransactionIsolation(level)
  def setTypeMap(map: java.util.Map[String, Class[_]]) = connection.setTypeMap(map)
  def isWrapperFor(c: Class[_]) = connection.isWrapperFor(c)
  def unwrap[T](c: Class[T]) = connection.asInstanceOf[T]

  def abort(executor: java.util.concurrent.Executor) = connection.abort(executor)
  def getNetworkTimeout = connection.getNetworkTimeout
  def setNetworkTimeout(executor: java.util.concurrent.Executor, timeout: Int) = connection.setNetworkTimeout(executor, timeout)
  def getSchema = connection.getSchema
  def setSchema(schema: String) = connection.setSchema(schema)

}