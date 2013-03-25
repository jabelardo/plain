package com.ibm

package plain

import java.util.concurrent.ForkJoinPool

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

  final val cores = sys.runtime.availableProcessors

  final val parallelism = cores * cores

  final def dispatcher = Concurrent.dispatcher

  final def scheduler = Concurrent.scheduler

  final def executor = Concurrent.executor

  final val ioexecutor = {
    import java.util.concurrent._
    val keepalive = 60L
    val maxqueuesize = 64 * 1024
    new ThreadPoolExecutor(
      cores,
      parallelism,
      keepalive,
      TimeUnit.SECONDS,
      new ArrayBlockingQueue[Runnable](maxqueuesize))
  }

  /**
   * Tired of typing 'Thread.'? Use this one. We won't win a Turing award with it for sure.
   */
  final def sleep(milliseconds: Long) = Thread.sleep(milliseconds)
  /**
   * Spawn a body: => Unit to an execution context and forget about it. Use this only if you have no need to handle errors during the execution of 'body'.
   */
  final def spawn(body: ⇒ Any): Unit = dispatcher.execute(new Runnable { def run = body })

  /**
   * Spawn a body: => Unit to an execution context and forget about it. This versions requires an explicit dispatcher, useful in combination with a PinnedDispatcher.
   */
  final def spawn(dispatcher: MessageDispatcher)(body: ⇒ Any): Unit = {
    dispatcher.execute(new Runnable { def run = body })
  }

  /**
   * Schedule 'body' to be executed every 'repeateddelay' milliseconds, but execute it first after 'initialdelay' milliseconds.
   */
  final def schedule(initialdelay: Long, repeateddelay: Long)(body: ⇒ Unit) = {
    scheduler.schedule(initialdelay.milliseconds, repeateddelay.milliseconds)(body)(dispatcher)
  }

  /**
   * Schedule 'body' to be executed every 'delay' milliseconds, but execute it first after 'delay' milliseconds.
   */
  final def schedule(delay: Long)(body: ⇒ Unit) = {
    scheduler.schedule(delay.milliseconds, delay.milliseconds)(body)(dispatcher)
  }

  /**
   * Schedule 'body' to be executed only once after 'initialdelay' milliseconds.
   */
  final def scheduleOnce(delay: Long)(body: ⇒ Unit): Cancellable = {
    scheduler.scheduleOnce(delay.milliseconds)(body)(dispatcher)
  }

  /**
   * Simply create a scala.dispatch.Future by providing a body: => T without worrying about an execution context.
   * Usually this is too simplistic, you will probably need Future/Promise to handle full asynchronicity.
   */
  final def future[T](body: ⇒ T): Future[T] = Future(body)(dispatcher)

  final val scheduleGcTimeout = getMilliseconds("plain.concurrent.schedule-gc-timeout", 60000) match {
    case timeout if 60000 <= timeout ⇒ timeout
    case _ ⇒ -1
  }

}
