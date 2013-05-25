package com.ibm

package plain

package continuation

/*
 * 
 */
sealed trait Trampoline[A] {

  import Trampoline._

  final def map[B](f: A ⇒ B): Trampoline[B] = flatMap(a ⇒ More(() ⇒ Done(f(a))))

  final def flatMap[B](f: A ⇒ Trampoline[B]): Trampoline[B] = Cont(this, f)

  final def run: A = {
    var cur: Trampoline[_] = this
    var stack: List[Any ⇒ Trampoline[A]] = Nil
    var result: Option[A] = None
    while (result.isEmpty) {
      cur match {
        case Done(a) ⇒ stack match {
          case Nil ⇒ result = Some(a.asInstanceOf[A])
          case c :: cs ⇒
            cur = c(a)
            stack = cs
        }
        case More(t) ⇒ cur = t()
        case Cont(a, f) ⇒
          cur = a
          stack = f.asInstanceOf[Any ⇒ Trampoline[A]] :: stack
      }
    }
    result.get
  }

}

object Trampoline {

  final case class Done[A](a: A) extends Trampoline[A]

  final case class More[A](a: () ⇒ Trampoline[A]) extends Trampoline[A]

  final case class Cont[A, B](a: Trampoline[A], f: A ⇒ Trampoline[B]) extends Trampoline[B]

  final def fib(n: Int): Trampoline[Int] = if (n < 2) Done(n) else for {
    x ← fib(n - 1)
    y ← fib(n - 2)
  } yield (x + y)

}
