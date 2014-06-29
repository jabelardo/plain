package com.ibm

package plain

import java.sql.Savepoint
import javax.sql.DataSource

import scala.collection.concurrent.TrieMap

import com.ibm.plain.jdbc.{ Connection, ConnectionFactory, DataSourceWrapper }

import config.{ CheckedConfig, config2RichConfig }
import bootstrap.Application

package object jdbc

    extends CheckedConfig {

  import config._
  import config.settings._

  final def connectionFactoryForName(name: String): Option[ConnectionFactory] = connectionfactoriescache.get(name) match {
    case e @ Some(_) ⇒ e
    case None ⇒ Application
      .instance
      .getComponents(classOf[ConnectionFactory])
      .find(_.asInstanceOf[ConnectionFactory].displayname == name) match {
        case Some(connectionfactory: ConnectionFactory) ⇒
          connectionfactoriescache.put(name, connectionfactory)
          Some(connectionfactory)
        case _ ⇒ None
      }
  }

  final def connectionForName(name: String): Option[Connection] = connectionFactoryForName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ connectionfactory.getConnection
    case _ ⇒ None
  }

  final def dataSourceForName(name: String): Option[DataSource] = connectionFactoryForName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ Some(new DataSourceWrapper(connectionfactory.dataSource, connectionfactory))
    case _ ⇒ None
  }

  final def connectionFactoryForJndiLookupName(name: String): Option[ConnectionFactory] = connectionfactoriescache.get(name) match {
    case e @ Some(_) ⇒ e
    case None ⇒ Application
      .instance
      .getComponents(classOf[ConnectionFactory])
      .find(_.asInstanceOf[ConnectionFactory].jndilookupyname == name) match {
        case Some(connectionfactory: ConnectionFactory) ⇒ Some(connectionfactory)
        case _ ⇒ None
      }
  }

  final def connectionForJndiLookupName(name: String): Option[Connection] = connectionFactoryForJndiLookupName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ connectionfactory.getConnection
    case _ ⇒ None
  }

  final def dataSourceForJndiLookupName(name: String): Option[DataSource] = connectionFactoryForJndiLookupName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ Some(new DataSourceWrapper(connectionfactory.dataSource, connectionfactory))
    case _ ⇒ None
  }

  /**
   * Example: withConnection("YOURDB") { implicit connection => "insert into test values (1, 'ONE')" <<!!; }
   *
   * This will only work if getAutoCommit == true, use withTransaction instead.
   *
   * withConnection will commit only if auto-commit is set to true after each sql statement, you will need to import scala.language.postfixOps to get rid of warnings
   * for methods like '!'.
   */
  final def withConnection[A](name: String)(body: Connection ⇒ A): A = connectionForName(name) match {
    case Some(connection) ⇒
      try body(connection) finally connection.close
    case _ ⇒
      throw new IllegalStateException("Could not create connection for : '" + name + "'")
  }

  /**
   * see also: withConnection
   *
   * Example: withTransaction("YOURDB") { implicit connection => savepoint =>  "insert into test values (1, 'ONE')" <<!!; connection.rollback(savepoint) }
   *
   * withTransaction will never commit or rollback automatically, it must be done inside 'body' to make things permanent.
   */
  final def withTransaction[A](name: String)(body: Connection ⇒ Savepoint ⇒ A): A = connectionForName(name) match {
    case Some(connection) ⇒
      val autocommit = connection.getAutoCommit
      if (autocommit) connection.setAutoCommit(false)
      val savepoint = connection.setSavepoint
      try body(connection)(savepoint) finally {
        if (autocommit) connection.setAutoCommit(true)
        connection.releaseSavepoint(savepoint)
        connection.close
      }
    case _ ⇒
      throw new IllegalStateException("Could not create connection for : '" + name + "'")
  }

  final val startupConnectionFactories: List[String] = getStringList("plain.jdbc.startup-connection-factories", List.empty)

  private[this] final val connectionfactoriescache = new TrieMap[String, ConnectionFactory]

}
