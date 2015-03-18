package com.ibm

package plain

import java.util.concurrent.{ ScheduledFuture, TimeUnit }

import scala.concurrent.Future
import scala.language.implicitConversions

import com.ibm.plain.concurrent.Concurrent

import config.CheckedConfig

import java.util.concurrent.{ ScheduledFuture, TimeUnit }

import scala.concurrent.Future
import scala.language.implicitConversions

import com.ibm.plain.concurrent.Concurrent

package object concurrent

    extends CheckedConfig {

  import config._
  import config.settings._

  final val cores = sys.runtime.availableProcessors

  final val parallelism = cores * 2

  final def executor = Concurrent.instance.executor

  final def scheduler = Concurrent.instance.scheduler

  final def ioexecutor = Concurrent.instance.ioexecutor

  /**
   * Spawn a body: => Unit to an execution context and forget about it. Use this only if you have no need to handle errors during the execution of 'body'.
   */
  final def spawn(body: ⇒ Any): Unit = executor.execute(body)

  /**
   * Schedule 'body' to be executed every 'repeateddelay' milliseconds, but execute it first after 'initialdelay' milliseconds.
   */
  final def schedule(initialdelay: Long, repeateddelay: Long)(body: ⇒ Any) = {
    scheduler.scheduleWithFixedDelay(body, initialdelay, repeateddelay, TimeUnit.MILLISECONDS)
  }

  /**
   * Schedule 'body' to be executed every 'delay' milliseconds, but execute it first after 'delay' milliseconds.
   */
  final def schedule(delay: Long)(body: ⇒ Any): ScheduledFuture[_] = {
    scheduler.scheduleWithFixedDelay(body, delay, delay, TimeUnit.MILLISECONDS)
  }

  /**
   * Schedule 'body' to be executed only once after 'initialdelay' milliseconds.
   */
  final def scheduleOnce(delay: Long)(body: ⇒ Any): ScheduledFuture[_] = {
    scheduler.schedule(new Runnable { def run = body }, delay, TimeUnit.MILLISECONDS)
  }

  /**
   * Simply create a scala.dispatch.Future by providing a body: => T without worrying about an execution context.
   * Usually this is too simplistic, you will probably need Future/Promise to handle full asynchronicity.
   */
  final def future[T](body: ⇒ T): Future[T] = Future(body)(executor)

  private[this] final implicit def runnable(body: ⇒ Any): Runnable = new Runnable { def run = body }

}
