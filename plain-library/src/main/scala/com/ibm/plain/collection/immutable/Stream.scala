package com.ibm

package plain

package collection

package immutable

object Stream {

  /**
   * Much cheaper but non-threadsafe implementation.
   */
  final class Cons[+A](hd: A, tl: ⇒ Stream[A])

    extends Stream[A] {

    override final val isEmpty = false

    override final val head = hd

    override final def tail: Stream[A] = tl

    protected final val tailDefined = true

  }

  object cons {

    final def apply[A](hd: A, tl: ⇒ Stream[A]) = new Cons(hd, tl)

  }

}
