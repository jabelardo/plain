package com.ibm

package plain

import java.sql.{ SQLException, Savepoint }

import com.ibm.plain.jdbc.{ Connection, ConnectionFactory, DataSourceWrapper }

import config.{ CheckedConfig, config2RichConfig }
import javax.sql.DataSource

package object jdbc

  extends CheckedConfig {

  import config._
  import config.settings._

  final def connectionFactoryForName(name: String): Option[ConnectionFactory] = bootstrap
    .application
    .getComponents(classOf[ConnectionFactory])
    .find(_.asInstanceOf[ConnectionFactory].displayname == name) match {
      case Some(connectionfactory: ConnectionFactory) ⇒ Some(connectionfactory)
      case _ ⇒ None
    }

  final def connectionForName(name: String): Option[Connection] = connectionFactoryForName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ connectionfactory.getConnection
    case _ ⇒ None
  }

  final def dataSourceForName(name: String): Option[DataSource] = connectionFactoryForName(name) match {
    case Some(connectionfactory: ConnectionFactory) ⇒ Some(new DataSourceWrapper(connectionfactory.dataSource, connectionfactory))
    case _ ⇒ None
  }

  /**
   * Example: withConnection("YOURDB") { implicit connection => "insert into test values (1, 'ONE')" <<!!; }
   *
   * withConnection will always auto-commit after each sql statement, you will need to import scala.language.postfixOps to get rid of warnings
   * for methods like <<!!.
   */
  final def withConnection[A](name: String)(body: Connection ⇒ A): A = connectionForName(name) match {
    case Some(connection) ⇒
      connection.setAutoCommit(true); try body(connection) finally connection.close
    case _ ⇒
      throw new SQLException("Could not create connection for : '" + name + "'")
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
      connection.setAutoCommit(false); val savepoint = connection.setSavepoint; try body(connection)(savepoint) finally connection.close
    case _ ⇒
      throw new SQLException("Could not create connection for : '" + name + "'")
  }

  final val startupConnectionFactories: List[String] = getStringList("plain.jdbc.startup-connection-factories", List.empty)

}
