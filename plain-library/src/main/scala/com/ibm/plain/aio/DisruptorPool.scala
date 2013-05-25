package com.ibm

package plain

package aio

import java.util.concurrent.atomic.AtomicBoolean

import com.lmax.disruptor._
import com.lmax.disruptor.dsl._

import scala.annotation.tailrec

import logging.HasLogger
import concurrent.OnlyOnce

final class DisruptorPool[A] private (factory: EventFactory[A], ringbuffersize: Int, initialpoolsize: Int)

  extends HasLogger

  with OnlyOnce {

  /**
   * This is an expensive O(n) operation.
   */
  final def size = pool.size

  @tailrec final def get: Disruptor[A] = if (trylock) {
    try pool match {
      case head :: tail ⇒
        pool = tail
        head
      case Nil ⇒
        onlyonce { warning("DisruptorPool exhausted : initial pool size " + initialpoolsize) }
        newDisruptor
    } finally unlock
  } else {
    Thread.sleep(0, 50)
    get
  }

  @tailrec final def release(disruptor: Disruptor[A]): Unit = if (trylock) {
    try {
      pool = disruptor :: pool
    } finally unlock
  } else {
    Thread.sleep(0, 50)
    release(disruptor)
  }

  @inline private[this] final def trylock = locked.compareAndSet(false, true)

  @inline private[this] final def unlock = locked.set(false)

  @inline private[this] final def newDisruptor: Disruptor[A] = {
    val d = new Disruptor(
      factory,
      ringbuffersize,
      concurrent.executor,
      ProducerType.SINGLE,
      new BusySpinWaitStrategy)
    d.start
    d
  }

  @volatile private[this] final var pool: List[Disruptor[A]] = (0 until initialpoolsize).map(_ ⇒ newDisruptor).toList

  private[this] final val locked = new AtomicBoolean(false)

}

object DisruptorPool {

  def apply[A](factory: EventFactory[A], ringbuffersize: Int, initialpoolsize: Int) = new DisruptorPool[A](factory, ringbuffersize, initialpoolsize: Int)

}
