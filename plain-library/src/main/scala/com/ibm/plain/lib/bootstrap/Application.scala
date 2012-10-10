package com.ibm.plain

package lib

package bootstrap

import scala.collection.mutable.{ ArrayBuffer, SynchronizedBuffer }
import scala.concurrent.util.Duration

import time.now
import concurrent.OnlyOnce

/**
 *
 */
abstract sealed class Application

  extends OnlyOnce {

  override def toString = components.toList.toString

  def bootstrap = components.filter(_.isEnabled).foreach(_.doStart)

  def teardown = onlyonce { components.filter(_.isStarted).reverse.foreach(_.doStop) }

  def awaitTermination(timeout: Duration) = components.filter(_.isStarted).reverse.foreach { c ⇒
    c.doAwaitTermination(timeout)
    teardown
  }

  def register(component: Component[_]): Application = { components += component.asInstanceOf[BaseComponent[_]]; this }

  def unregister(component: Component[_]) = { components -= component.asInstanceOf[BaseComponent[_]]; this }

  def uptime = now - starttime

  private[this] final val components = new ArrayBuffer[BaseComponent[_]] with SynchronizedBuffer[BaseComponent[_]]

  private[this] final val starttime = now
}

/**
 * The Application object.
 */
object Application extends Application
