
package com.ibm.plain

package lib

import scala.collection.mutable.{ ArrayBuffer, SynchronizedBuffer }
import scala.concurrent.Future
import scala.concurrent.util.Duration
import scala.concurrent.util.duration.longToDurationLong

import akka.actor.{ ActorSystem, Cancellable }
import akka.dispatch.MessageDispatcher
import config.config2RichConfig

package object concurrent

  extends config.CheckedConfig {

  import config._
  import config.settings._

  /**
   * Spawn a body: => Unit to an execution context and forget about it. Use this only if you have no need to handle errors during the execution of 'body'.
   */
  def spawn(body: ⇒ Any): Unit = actorSystem.dispatcher.execute(new Runnable { def run = body })

  /**
   * Spawn a body: => Unit to an execution context and forget about it. This versions requires an explicit dispatcher, useful in combination with a PinnedDispatcher.
   */
  def spawn(dispatcher: MessageDispatcher)(body: ⇒ Any): Unit = {
    dispatcher.execute(new Runnable { def run = body })
  }

  /**
   * Schedule 'body' to be executed every 'repeateddelay' milliseconds, but execute it first after 'initialdelay' milliseconds.
   */
  def schedule(initialdelay: Long, repeateddelay: Long)(body: ⇒ Unit) = {
    actorSystem.scheduler.schedule(initialdelay.milliseconds, repeateddelay.milliseconds)(body)(actorSystem.dispatcher)
  }

  /**
   * Schedule 'body' to be executed only once after 'initialdelay' milliseconds.
   */
  def scheduleOnce(delay: Long)(body: ⇒ Unit): Cancellable = {
    actorSystem.scheduler.scheduleOnce(delay.milliseconds)(body)(actorSystem.dispatcher)
  }

  /**
   * Simply create a [[scala.dispatch.Future]] by providing a body: => T without worrying about an execution context.
   * Usually this is too simplistic, you will probably need Future/Promise to handle full asynchronicity.
   */
  def future[T](body: ⇒ T): Future[T] = {
    Future(body)(actorSystem.dispatcher)
  }

  /**
   * Add a piece of code (body) to be executed at Runtime.exit. All pieces of code will be executed in one thread at reverse order.
   */
  def addShutdownHook(body: ⇒ Unit): Unit = {
    shutdownHooks += (() ⇒ body)
    if (shutdownHooks.isEmpty) Runtime.getRuntime.addShutdownHook(new Thread(new Runnable { def run = runShutdownHooks }))
  }

  def shutdown = {
    runShutdownHooks
    actorSystem.shutdown
  }

  def isTerminated = actorSystem.isTerminated

  def awaitTermination(timeout: Duration) = actorSystem.awaitTermination(timeout)

  def awaitTermination = actorSystem.awaitTermination(Duration.Inf)

  implicit final val actorSystem = {
    val system = ActorSystem(getString("plain.concurrent.actorsystem", "default"), config.settings)
    system.registerOnTermination(logging.shutdown)
    addShutdownHook(system.shutdown)
    system
  }

  private[this] def runShutdownHooks = {
    val hooks = shutdownHooks.toList
    shutdownHooks.clear
    hooks.reverse.foreach { p ⇒
      try {
        p()
      } catch {
        case e: Throwable ⇒ println("shutdownHook : " + e)
      }
    }

  }

  private[this] final lazy val shutdownHooks = new ArrayBuffer[() ⇒ Unit] with SynchronizedBuffer[() ⇒ Unit]

}
