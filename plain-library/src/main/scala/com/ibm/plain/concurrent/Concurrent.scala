package com.ibm

package plain

package concurrent

import java.util.concurrent.{ Executors, ForkJoinPool, TimeUnit }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import bootstrap.BaseComponent

/**
 * Just needed for inheritance.
 */
abstract sealed class Concurrent

  extends BaseComponent[Concurrent]("plain-concurrent")

  with OnlyOnce {

  override final def stop = {
    if (isStarted) {
      forkjoinpool.shutdown
      scheduledpool.shutdown
      ignore(Thread.sleep(bootstrap.delayDuringTeardown))
      forkjoinpool.shutdownNow
      scheduledpool.shutdownNow
    }
    this
  }

  override final def awaitTermination(timeout: Duration) = ignore(forkjoinpool.awaitTermination(timeout.toMillis, TimeUnit.MILLISECONDS))

  final def scheduler = scheduledpool

  final def executor = executioncontext

  final def ioexecutor = forkjoinpool

  private[this] final val scheduledpool = Executors.newScheduledThreadPool(cores)

  private[this] final val forkjoinpool = new ForkJoinPool(parallelism)

  private[this] final val executioncontext: ExecutionContext = ExecutionContext.fromExecutorService(forkjoinpool)

}

/**
 * The Concurrent object.
 */
object Concurrent extends Concurrent
