package com.ibm

package plain

package integration

package distributedconfig

import org.apache.camel.scala.dsl.builder.RouteBuilder

import bootstrap.ExternalComponent
import logging.Logger

/**
 *
 */
final class DistributedConfig

  extends ExternalComponent[DistributedConfig]("plain-integration-distributed-config")

  with Logger {

  override def order = bootstrapOrder

  override def start = {
    if (null == configcontext) {
      (if (isMaster) masterRoutes else slaveRoutes).addRoutesToCamelContext(camel.context)
      configcontext = this
    }
    this
  }

  override def stop = {
    if (null != configcontext) {
      camel.context.stopRoute(if (isMaster) "distributed-config-master-routes" else "distributed-config-slave-routes")
      configcontext = null
    }
    this
  }

  private[this] final def masterRoutes = new RouteBuilder {

    from("servlet:/distributed-config?matchOnUriPrefix=true").
      routeId("distributed-config-master-routes").
      setBody(settings.root.render, classOf[String])

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


