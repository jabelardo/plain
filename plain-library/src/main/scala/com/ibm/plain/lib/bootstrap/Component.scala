package com.ibm.plain

package lib

package bootstrap

import java.util.concurrent.TimeoutException

import scala.concurrent.util.Duration

/**
 * A Component will be started automatically by the bootstrapping mechanism if it is enabled which it is by default.
 * It will also be stopped by the tear down mechanism. All components are registered automatically with bootstrap and tear down.
 */
trait Component[C] {

  val name: String

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
abstract class BaseComponent[C](val name: String)

  extends Component[C] {

  override def toString = "Component(name:" + name + ", enabled:" + isEnabled + ", started:" + isStarted + ", stopped:" + isStopped + ")"

  def isEnabled = enabled

  def enable = { enabled = true; this.asInstanceOf[C] }

  def disable = {
    if (isEnabled) stop
    enabled = false
    this.asInstanceOf[C]
  }

  def doStart = try {
    start
  } catch {
    case e: Throwable ⇒ println("Excption during start of '" + name + "' : " + e)
  }

  def doStop = try {
    stop
  } catch {
    case e: Throwable ⇒ println("Excption during stop of '" + name + "' : " + e)
  }

  def doAwaitTermination(timeout: Duration) = try {
    awaitTermination(timeout)
  } catch {
    case e: TimeoutException ⇒ println(name + " " + e)
    case e: Throwable ⇒ println("Excption during awaitTermination of '" + name + "' : " + e)
  }

  @volatile private[this] var enabled = true

}
