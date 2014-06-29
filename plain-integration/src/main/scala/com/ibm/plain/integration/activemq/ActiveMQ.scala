package com.ibm

package plain

package integration

package activemq

import java.io.File

import org.apache.camel.scala.dsl.builder.RouteBuilder
import org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent
import org.apache.activemq.broker.BrokerService
import org.apache.commons.io.FileUtils.deleteDirectory

import bootstrap.{ ExternalComponent, Singleton }
import logging.Logger
import camel.Camel
import distributedconfig.DistributedConfig

/**
 *
 */
final class ActiveMQ

    extends ExternalComponent[ActiveMQ](

      activemq.isEnabled,

      "plain-integration-activemq",

      classOf[infrastructure.Infrastructure])

    with Logger {

  override def start = {
    if (null == broker) {
      if (isMaster) {
        broker = new BrokerService
        if (usePersistence) {
          val directory = new File(persistenceDirectory)
          if (purgePersistenceDirectoryOnStartup) deleteDirectory(directory)
          broker.getPersistenceAdapter.setDirectory(directory)
          broker.setPersistent(true)
        } else {
          broker.setPersistent(false)
        }
        broker.setBrokerName(name)
        broker.setUseShutdownHook(true)
        broker.setUseJmx(true)
        broker.addConnector(brokerServerUri + ":" + brokerPort)
        broker.start
        broker.waitUntilStarted
      } else {
        Camel.instance.context.addComponent("activemq", activeMQComponent(brokerClientUri + ":" + brokerPort))

        new RouteBuilder {

          from("timer:foo?period=20s").
            transform("this is a text message!").
            to("activemq:queue:inbox")

          from("activemq:queue:inbox").
            to("mock:result").
            to("activemq:queue:outbox")

        }.addRoutesToCamelContext(Camel.instance.context)
      }
      ActiveMQ.instance(this)
    }
    this
  }

  override def stop = {
    if (null != broker) {
      ActiveMQ.resetInstance
      broker.stop
      broker.waitUntilStopped
      broker = null
    }
    this
  }

  final def brokerService = Option(broker)

  private[this] final var broker: BrokerService = null

}

/**
 *
 */
object ActiveMQ

  extends Singleton[ActiveMQ]
