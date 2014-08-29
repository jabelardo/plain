package com.ibm.plain
package integration
package catia5

import scala.collection.JavaConversions.asScalaBuffer

import org.apache.camel.{ Consumer, Endpoint, Exchange, PollingConsumer, Processor, Producer }
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.impl.{ DefaultComponent, DefaultConsumer, DefaultEndpoint, DefaultProducer }
import org.apache.camel.model.{ ModelCamelContext, RoutesDefinition }
import org.apache.camel.test.junit4.CamelTestSupport

import camel.Camel
import logging.Logger

/**
 * Represents the component that manages {@link Catia5Endpoint}.
 */
final class Catia5Component

    extends DefaultComponent {

  protected final def createEndpoint(uri: String, remaining: String, parameters: java.util.Map[String, Object]): Endpoint = {
    val endpoint = new Catia5Endpoint(uri, this)
    setProperties(endpoint, parameters)
    endpoint
  }

}

/**
 * Represents a Catia5 endpoint.
 */
final class Catia5Endpoint(

  uri: String,

  component: Catia5Component)

    extends DefaultEndpoint(uri, component) {

  final def this() = this(null, null)

  final def this(uri: String) = this(uri, null)

  final def createProducer: Producer = {
    new Catia5Producer(this);
  }

  final def createConsumer(processor: Processor): Consumer = {
    new Catia5Consumer(this, processor);
  }

  final def isSingleton = true

}

/**
 * The Catia5 producer.
 */
final class Catia5Producer(

  endpoint: Catia5Endpoint)

    extends DefaultProducer(endpoint)

    with Logger {

  final def process(exchange: Exchange): Unit = {
    println("##2 producer " + exchange)
    println(exchange.getIn().getBody());
  }

}

/**
 * The Catia5 consumer.
 */
final class Catia5Consumer(

  endpoint: Catia5Endpoint,

  processor: Processor)

    extends DefaultConsumer(endpoint, processor)

    with PollingConsumer

    with Logger {

  println("##1 consumer")

  override final def doStart: Unit = {
    println("##3 consumer")
    super.doStart
    val builder = new RouteBuilder {
      def configure {
        from("activemq:/catia5inbox").to("stream:out")
      }
    }
    builder.addRoutesToCamelContext(Camel.instance.context)
    routes = builder.getRouteCollection
  }

  override final def doStop: Unit = {
    println("##2 consumer")
    routes.getRoutes.map(Camel.instance.context.stopRoute)
    super.doStop
  }

  final def receive: Exchange = {
    println("receive")
    null
  }

  final def receive(timeout: Long): Exchange = {
    println("receive timeout")
    null

  }

  final def receiveNoWait: Exchange = {
    println("receive nowait")
    null
  }

  private[this] final var routes: RoutesDefinition = null
}

/**
 * Testing.
 */
final class Catia5ComponentTest(camelcontext: ModelCamelContext)

    extends CamelTestSupport {

  this.context = camelcontext

  final def testCatia5 {
    val mock = getMockEndpoint("mock:result")
    mock.expectedMinimumMessageCount(1)
    assertMockEndpointsSatisfied
  }

  override protected final def createRouteBuilder: RouteBuilder = {
    new RouteBuilder {
      def configure {
        from("catia5://foo").
          to("catia5://bar").
          to("mock:result")
      }
    }
  }

}
