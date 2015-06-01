package com.ibm.plain
package integration
package spaces

import java.net.URI

import scala.beans.BeanProperty
import scala.math.max

import org.apache.camel.{ Consumer, Endpoint, Exchange, Processor, Producer }
import org.apache.camel.impl.{ DefaultComponent, DefaultEndpoint, DefaultProducer }
import org.apache.camel.spi.{ UriEndpoint, UriParam }

import bootstrap.Application
import logging.Logger
import concurrent.spawn

/**
 * Add plain controlling to a camel route, eg. like 'shutdown' at the end of a route.
 */
final class PlainComponent

    extends DefaultComponent

    with Logger {

  protected final def createEndpoint(uri: String, remaining: String, parameters: java.util.Map[String, Object]): Endpoint = {
    val endpoint = new PlainEndpoint(uri, this)
    setProperties(endpoint, parameters)
    endpoint
  }

}

/**
 * Represents a Plain endpoint.
 */
@UriEndpoint(scheme = "plain" /*, title = "plain", syntax = "plain:path"*/ )
class PlainEndpoint(

  uri: String,

  component: PlainComponent)

    extends DefaultEndpoint(uri, component)

    with Logger {

  final def this() = this(null, null)

  final def this(uri: String) = this(uri, null)

  final def createProducer: Producer = {
    new PlainProducer(this)
  }

  final def createConsumer(processor: Processor): Consumer = {
    throw new InstantiationException(s"createConsumer is not implemented for $this")
  }

  final def isSingleton = true

  /**
   * Parameters
   */

  final def getMethodName = {
    (if (null == method) {
      val u = URI.create(uri)
      method = u.getAuthority match { case null ⇒ u.getPath case a ⇒ a }
      require(null != method, s"Cannot deduct a valid method name from this uri : $u")
      method
    } else {
      method
    }).toLowerCase
  }

  @BeanProperty @UriParam protected[this] final var method: String = null

  @BeanProperty @UriParam protected[this] final var delay: Int = 0

}

/**
 * The Plain producer.
 */
final class PlainProducer(

  endpoint: PlainEndpoint)

    extends DefaultProducer(endpoint)

    with Logger {

  final def process(exchange: Exchange): Unit = {
    val method = endpoint.getMethodName
    val delay = max(endpoint.getDelay, 1000)
    method match {
      case "shutdown" ⇒ spawn {
        info(s"Shutdown initiated in $delay ms.")
        Thread.sleep(delay)
        Application.instance.teardown
      }
      case m ⇒ error(s"Method not implemented : $m")
    }
  }

}
