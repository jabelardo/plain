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

    override val isEmpty = false

    override val head = hd

    override def tail: Stream[A] = tl

    protected val tailDefined = true

  }

  object cons {

    def apply[A](hd: A, tl: ⇒ Stream[A]) = new Cons(hd, tl)

  }

}
