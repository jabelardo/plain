package com.ibm

package plain

package aio

import com.lmax.disruptor.dsl._
import com.lmax.disruptor.{ EventFactory ⇒ Factory, EventHandler ⇒ Handler, EventTranslatorOneArg ⇒ Translator }
import com.lmax.disruptor.SleepingWaitStrategy

/**
 *
 */
final class IterateeDisruptor {

  final type K = Input[Any] ⇒ (Iteratee[Any, Any], Input[Any])

  final type F = Any ⇒ Iteratee[Any, Any]

  final def publish(i: Int, k: Any, f: Any) = disruptor.publishEvent(translator, (i, k.asInstanceOf[K], f.asInstanceOf[F]))

  private final class Event(var e: (Int, K, F))

  private final class EventFactory

    extends Factory[Event] {

    final def newInstance = new Event(null)

  }

  private final class EventTranslator

    extends Translator[Event, (Int, K, F)] {

    final def translateTo(event: Event, sequence: Long, e: (Int, K, F)) = {
      event.e = e
    }
  }

  private final class EventHandler

    extends Handler[Event] {

    final def onEvent(event: Event, sequence: Long, endofbatch: Boolean) = {
      println(sequence + " i=" + event.e._1)
    }

  }

  private[this] final val factory = new EventFactory

  private[this] final val translator = new EventTranslator

  private[this] final val (ringbuffer, disruptor) = {
    val d = new Disruptor(factory, 1024, concurrent.executor, ProducerType.MULTI, new SleepingWaitStrategy)
    d.handleEventsWith(new EventHandler)
    (d.start, d)
  }

}
