package com.ibm

package plain

package integration

package distributedconfig

import org.apache.camel.scala.dsl.builder.RouteBuilder

import bootstrap.{ Component, ExternalComponent, Singleton }
import logging.Logger
import camel.Camel

/**
 *
 */
final class DistributedConfig

    extends ExternalComponent[DistributedConfig](

      distributedconfig.isEnabled,

      "plain-integration-distributed-config",

      classOf[Camel])

    with Logger {

  override def start = {
    DistributedConfig.instance(this)
    this
  }

  override def stop = {
    DistributedConfig.resetInstance
    this
  }

}

/**
 *
 */
object DistributedConfig

  extends Singleton[DistributedConfig]
