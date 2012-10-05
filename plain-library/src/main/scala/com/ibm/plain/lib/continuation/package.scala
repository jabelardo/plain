package com.ibm.plain

package lib

/**
 * Implements the "Disposable pattern" which is "sort-of" providing destructors for languages with garbage collection.
 *
 * @see [[http://blog.omega-prime.co.uk/?p=17 Inspired by "using" keyword in C#]]
 */
package object continuation {

  import language.implicitConversions
  import language.reflectiveCalls

  import scala.util.continuations.{ ControlContext, cps, shift, reset }

  /**
   * Has a method "dispose" that will be *automatically* called at the end of a "using" block.
   */
  trait Disposable[S] {
    def dispose
  }

  trait ContextType[T]

  def forceContextType[T]: ContextType[T] = null

  private def use[S <% Disposable[S], T: ContextType](what: S)(block: S ⇒ T): T = {
    try {
      block(what)
    } catch {
      case e: OutOfMemoryError ⇒ bootstrap.terminateJvm(e, -1)
      case e: Throwable ⇒ throw e
    } finally {
      what.dispose
    }
  }

  /**
   * @return The input wrapped as a Disposable, necessary for all things you want to dispose after "using".
   */
  def disposable[S <% Disposable[S], T: ContextType](what: S): S @cps[T] = shift(use[S, T](what))

  /**
   * Syntactic sugar to support code like
   * {{{
   * implicit val _ = forceContextType[Unit]
   * using {
   *     val representation = disposable(perLocation(location, filesperlocation))
   *     val zipin = disposable(new ZipInputStream(representation.getStream))
   *     var zipentry: ZipEntry = null
   *     while (null != { zipentry = zipin.getNextEntry; zipentry }) {
   *         addZipEntry(zipentry.getName) {
   *             copyBytes(zipin, zipout)
   * 	           files.append(zipentry.getName)
   * 	       }
   *     }
   * }
   * }}}
   * @return T of forceContextType[T]
   */
  def using[T](block: ⇒ T @cps[T]): T = reset { block }

  // implicit conversions

  implicit def dispose2disposable[S](what: S { def dispose() }): Disposable[S] = {
    new Disposable[S] {
      def dispose = what.dispose
    }
  }

  implicit def close2disposable[S](what: S { def close() }): Disposable[S] = {
    new Disposable[S] {
      def dispose = {
        try {
          what.close
        } catch {
          case e: Throwable ⇒
        }
      }
    }
  }

  implicit def finish2disposable[S](what: S { def close(); def closeEntry(); def finish() }): Disposable[S] = {
    new Disposable[S] {
      def dispose = {
        try {
          what.finish
        } catch {
          case e: Throwable ⇒
        }
      }
    }
  }

  implicit def exhaust2disposable[S](what: S { def exhaust(): Long; def release() }): Disposable[S] = {
    new Disposable[S] {
      def dispose = {
        what.exhaust
        what.release
      }
    }
  }

  implicit def closenobrackets2disposable[S](what: S { def close }): Disposable[S] = {
    new Disposable[S] {
      def dispose = {
        try {
          what.close
        } catch {
          case e: Throwable ⇒
        }
      }
    }
  }

  implicit def force2disposable[A](what: Stream[A]): Disposable[Stream[A]] = {
    new Disposable[Stream[A]] {
      def dispose = {
        what.force
        ()
      }
    }
  }

  implicit def finishclose2disposable[S](what: S { def close(); def finish() }): Disposable[S] = {
    new Disposable[S] {
      def dispose = {
        try {
          what.finish
          what.close
        } catch {
          case e: Throwable ⇒
        }
      }
    }
  }

}
