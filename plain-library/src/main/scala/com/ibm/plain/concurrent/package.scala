package com.ibm

package plain

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

import akka.actor.Cancellable
import akka.dispatch.MessageDispatcher

import logging.HasLogger
import config.CheckedConfig

package object concurrent

  extends CheckedConfig

  with HasLogger {

  import config._
  import config.settings._

  def dispatcher = Concurrent.dispatcher

  def scheduler = Concurrent.scheduler

  def executor = Concurrent.executor

  val ioexecutor = {
    import java.util.concurrent._
    val cores = sys.runtime.availableProcessors
    val parallelism = 32
    val keepalive = 60L
    val maxqueuesize = 64 * 1024
    new ThreadPoolExecutor(
      cores,
      cores * parallelism,
      keepalive,
      TimeUnit.SECONDS,
      new ArrayBlockingQueue[Runnable](maxqueuesize))
  }

  /**
   * Tired of typing 'Thread.'? Use this one. We won't win a Turing award with it for sure.
   */
  def sleep(milliseconds: Long) = Thread.sleep(milliseconds)
  /**
   * Spawn a body: => Unit to an execution context and forget about it. Use this only if you have no need to handle errors during the execution of 'body'.
   */
  def spawn(body: ⇒ Any): Unit = dispatcher.execute(new Runnable { def run = body })

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
    scheduler.schedule(initialdelay.milliseconds, repeateddelay.milliseconds)(body)(dispatcher)
  }

  /**
   * Schedule 'body' to be executed only once after 'initialdelay' milliseconds.
   */
  def scheduleOnce(delay: Long)(body: ⇒ Unit): Cancellable = {
    scheduler.scheduleOnce(delay.milliseconds)(body)(dispatcher)
  }

  /**
   * Simply create a [[scala.dispatch.Future]] by providing a body: => T without worrying about an execution context.
   * Usually this is too simplistic, you will probably need Future/Promise to handle full asynchronicity.
   */
  def future[T](body: ⇒ T): Future[T] = {
    Future(body)(dispatcher)
  }

}
