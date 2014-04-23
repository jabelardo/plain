package com.ibm

package plain

package bootstrap

import java.util.concurrent.TimeoutException

import scala.concurrent.duration.Duration

import logging.Logger

/**
 * A Component will be started automatically by the bootstrapping mechanism if it is enabled which it is by default.
 * It will also be stopped by the tear down mechanism. All components are registered automatically with bootstrap and tear down.
 */
trait Component[C] {

  def name: String

  def isStarted: Boolean

  def isStopped: Boolean

  def start: C

  def stop: C

  def awaitTermination(timeout: Duration)

  def isEnabled: Boolean

  def enable: C

  def disable: C

  def dependencies: Seq[Class[_ <: Component[_]]]

}

/**
 * Base implementation for a Component.
 */
abstract class BaseComponent[C](

  isenabled: Boolean,

  componentname: String,

  dependants: Class[_ <: Component[_]]*)

  extends Component[C] {

  final def this(name: String) = this(true, name, Seq.empty: _*)

  final def this() = this(null)

  def name = componentname

  final def isEnabled = enabled

  final def isStarted = !isStopped

  def isStopped = !started

  def preStart = ()

  def start = { started = true; this.asInstanceOf[C] }

  def stop = { started = false; this.asInstanceOf[C] }

  def awaitTermination(timeout: Duration) = ()

  def enable = { enabled = true; this.asInstanceOf[C] }

  def disable = {
    if (isEnabled) doStop
    enabled = false
    this.asInstanceOf[C]
  }

  final def dependencies: Seq[Class[_ <: Component[_]]] = dependants

  final def doStart = try {
    if (isEnabled && !isStarted) {
      start
      started = true
      info("Started component : " + name)
    }
  } catch {
    case e: Throwable ⇒ System.err.println("Exception during start of Component '" + name + "' : " + e); e.printStackTrace; throw e
  }

  final def doStop = try {
    if (isStarted) {
      stop
      started = false
      ignore(Thread.sleep(delayDuringTeardown))
      info("Stopped component : " + name)
    }
  } catch {
    case e: Throwable ⇒ System.err.println("Exception during stop of Component '" + name + "' : " + e)
  }

  final def doAwaitTermination(timeout: Duration) = try {
    if (isStarted) awaitTermination(timeout)
  } catch {
    case e: TimeoutException ⇒ System.err.println(name + " " + e)
    case e: Throwable ⇒ System.err.println("Exception during awaitTermination of Component '" + name + "' : " + e)
  }

  private[this] final def error(msg: String) = this match {
    case logger: Logger ⇒ logger.error(msg)
    case _ ⇒ System.err.println(msg)
  }

  private[this] final def warn(msg: String) = this match {
    case logger: Logger ⇒ logger.warn(msg)
    case _ ⇒ System.err.println(msg)
  }

  private[this] final def info(msg: String) = this match {
    case logger: Logger ⇒ logger.info(msg)
    case _ ⇒
  }

  private[this] final def debug(msg: String) = this match {
    case logger: Logger ⇒ logger.debug(msg)
    case _ ⇒
  }

  override def toString = "Component(name:" + name + ", enabled:" + isEnabled + ", started:" + isStarted + ", stopped:" + isStopped + ")"

  @volatile private[this] final var enabled = isenabled

  @volatile private[this] final var started = false

}
