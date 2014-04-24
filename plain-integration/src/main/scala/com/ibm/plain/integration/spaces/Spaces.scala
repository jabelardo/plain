package com.ibm

package plain

package integration

package spaces

import bootstrap.{ ExternalComponent, Singleton }
import logging.Logger
import distributedconfig.DistributedConfig

final class Spaces

  extends ExternalComponent[Spaces](

    spaces.isEnabled,

    "plain-integration-spaces",

    classOf[DistributedConfig])

  with Logger {

  override def order = bootstrapOrder

  override def start = {
    Spaces.instance(this)
    this
  }

  override def stop = {
    Spaces.resetInstance
    this
  }

}

/**
 *
 */
object Spaces

  extends Singleton[Spaces]
