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

    val i = 5

    from("servlet:/distributed-config?matchOnUriPrefix=true").
      routeId("distributed-config-master-routes").
      setBody(settings.root.render, classOf[String]).
      to("file:/tmp/input").
      to("file:/tmp/outout").
      wireTap("direct:wire").
      choice.
      when(i < 9).to("direct:b").to("file:/tmp/blabla").to("direct:eof").
      when(i == 9).to("direct:c").
      choice.
      when(2 < 3).to("direct:e").
      otherwise.to("direct:f").
      multicast.to("direct:x", "direct:y", "direct:z").
      loadBalance.failover(-1, false, true).
      to("direct:bad", "direct:bad2", "direct:good", "direct:good2")

    /**
     * sample routes
     *
     */
    from("servlet:/create-job-type-1?matchOnUriPrefix=true").
      routeId("create-job-type-1").
      setBody(settings.root.render, classOf[String]).
      to("file:/tmp/input")

  }

  private[this] final def slaveRoutes = new RouteBuilder {

    from("timer://distributedconfig?" + (if (polling) "fixedRate=true" else "repeatCount=1") + "&period=" + pollingTimeout + "&delay=" + startDelayedTimeout).
      routeId("distributed-config-slave-routes").
      to("ahc:http://" + masterHost + ":" + masterPort + "/integration-services/distributed-config").
      convertBodyTo(classOf[String]).
      to("mock:result").choice.
      when("type1".length > 0).to("mock:result").
      when("type2".length > 0).to("mock:result").
      to("mock:result");

  }

  private[this] final val settings = config.settings

}

/**
 *
 */
object DistributedConfig

  extends Singleton[DistributedConfig]
