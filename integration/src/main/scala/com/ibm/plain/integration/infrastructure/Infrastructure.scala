package com.ibm

package plain

package integration

package infrastructure

import bootstrap.{ ExternalComponent, Singleton }
import logging.Logger
import distributedconfig.DistributedConfig

/**
 *
 */
final class Infrastructure

    extends ExternalComponent[Infrastructure](

      infrastructure.isEnabled,

      "plain-integration-infrastructure",

      classOf[DistributedConfig])

    with Logger {

  override def start = {
    Infrastructure.instance(this)
    this
  }

  override def stop = {
    Infrastructure.resetInstance
    this
  }

}

/**
 *
 */
object Infrastructure

  extends Singleton[Infrastructure]
