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

}

/**
 * Base implementation for a Component.
 */
abstract class BaseComponent[C](n: String)

  extends Component[C] {

  def this() = this(null)

  def name = n

  override def toString = "Component(name:" + name + ", enabled:" + isEnabled + ", started:" + isStarted + ", stopped:" + isStopped + ")"

  def isEnabled = enabled

  def isStarted = started

  def isStopped = !started

  def start = { started = true; this.asInstanceOf[C] }

  def stop = { started = false; this.asInstanceOf[C] }

  def awaitTermination(timeout: Duration) = ()

  def enable = { enabled = true; this.asInstanceOf[C] }

  def disable = {
    if (isEnabled) stop
    enabled = false
    this.asInstanceOf[C]
  }

  def doStart = try {
    if (isEnabled && !isStarted) {
      start
      started = true
      info("Started component : " + name)
    }
  } catch {
    case e: Throwable ⇒ System.err.println("Exception during start of Component '" + name + "' : " + e); e.printStackTrace; throw e
  }

  def doStop = try {
    if (isStarted) {
      stop
      started = false
      ignore(Thread.sleep(delayDuringTeardown))
      info("Stopped component : " + name)
    }
  } catch {
    case e: Throwable ⇒ System.err.println("Exception during stop of Component '" + name + "' : " + e)
  }

  def doAwaitTermination(timeout: Duration) = try {
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

  @volatile private[this] var enabled = true

  @volatile private[this] var started = false

}
