package com.ibm

package plain

package integration

package distributedconfig

import org.apache.camel.scala.dsl.builder.RouteBuilder

import bootstrap.ExternalComponent
import camel.context
import logging.Logger

/**
 *
 */
final class ActiveMQ

  extends ExternalComponent[ActiveMQ]("plain-integration-activemq")

  with Logger {

  override def order = bootstrapOrder

  override def start = {
    this
  }

}


