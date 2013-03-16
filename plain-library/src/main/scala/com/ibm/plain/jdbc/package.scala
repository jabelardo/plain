package com.ibm

package plain

import javax.sql.DataSource

import config.CheckedConfig

package object jdbc

  extends CheckedConfig {

  import config._
  import config.settings._

  final def dataSourceFromConnectionFactory(displayname: String): Option[DataSource] = bootstrap
    .application
    .getComponents(classOf[ConnectionFactory])
    .find(_.asInstanceOf[ConnectionFactory].displayname == displayname) match {
      case Some(connectionfactory: ConnectionFactory) ⇒ Some(connectionfactory.dataSource)
      case _ ⇒ None
    }

  final val startupConnectionFactories: List[String] = getStringList("plain.jdbc.startup-connection-factories", List.empty)

}