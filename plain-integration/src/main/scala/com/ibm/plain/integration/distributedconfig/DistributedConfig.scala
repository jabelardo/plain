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
final class DistributedConfig

  extends ExternalComponent[DistributedConfig]("plain-integration-distributed-config")

  with Logger {

  override def order = bootstrapOrder

  override def start = {
    (if (isMaster) {
      new RouteBuilder {
        from("servlet:/distributed-config?matchOnUriPrefix=true").
          setBody(settings.root.render, classOf[String]).
          to("mock:result")
      }
    } else {
      val trigger = if (polling) "trigger.repeatInterval=" + pollingTimeout + "&trigger.repeatCount=100000" else "trigger.repeatCount=0"
      new RouteBuilder {
        from("quartz2://distributedconfig?fireNow=true&" + trigger + "&startDelayedSeconds=" + startDelayedTimeout).
          to("ahc:http://localhost:7070/integration-services/distributed-config?blabla").
          convertBodyTo(classOf[String]).
          to("mock:result")
      }
    }).addRoutesToCamelContext(context)
    this
  }

  private[this] final val settings = config.settings

}


