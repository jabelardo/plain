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
    (if (isMaster) masterRoutes else slaveRoutes).addRoutesToCamelContext(Camel.instance.context)
    DistributedConfig.instance(this)
    this
  }

  override def stop = {
    DistributedConfig.resetInstance
    Camel.instance.context.stopRoute(if (isMaster) "distributed-config-master-routes" else "distributed-config-slave-routes")
    this
  }

  private[this] final def masterRoutes = new RouteBuilder {

    from("servlet:/distributed-config?matchOnUriPrefix=true").
      routeId("distributed-config-master-routes").
      setBody(settings.root.render, classOf[String]).
      to("file:/tmp/input").
      to("file:/tmp/outout").
      to("direct:/totaltoll")

  }

  private[this] final def slaveRoutes = new RouteBuilder {

    from("timer://distributedconfig?" + (if (polling) "fixedRate=true" else "repeatCount=1") + "&period=" + pollingTimeout + "&delay=" + startDelayedTimeout).
      routeId("distributed-config-slave-routes").
      to("ahc:http://" + masterHost + ":" + masterPort + "/integration-services/distributed-config").
      convertBodyTo(classOf[String]).
      to("mock:result")

  }

  private[this] final val settings = config.settings

}

/**
 *
 */
object DistributedConfig

  extends Singleton[DistributedConfig]
