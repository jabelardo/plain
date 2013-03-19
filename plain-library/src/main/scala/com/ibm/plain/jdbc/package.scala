package com.ibm

package plain

import javax.sql.DataSource

import config.CheckedConfig

package object jdbc

  extends CheckedConfig {

  import config._
  import config.settings._

  final def connectionFactoryForName(displayname: String): Option[ConnectionFactory] = bootstrap
    .application
    .getComponents(classOf[ConnectionFactory])
    .find(_.asInstanceOf[ConnectionFactory].displayname == displayname) match {
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

  final def withConnection(name: String)(body: Connection ⇒ Unit): Unit = { body(connectionForName(name).get) }

  final val startupConnectionFactories: List[String] = getStringList("plain.jdbc.startup-connection-factories", List.empty)

}