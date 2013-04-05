package com.ibm

package plain

package hybriddb

package schema

import scala.language.implicitConversions
import scala.collection.Iterator

/**
 *
 */
package object column {

  final implicit def intToLongIterator(iter: Iterator[Int]) = new Iterator[Long] {

    @inline final def hasNext = iter.hasNext

    @inline final def next = iter.next.toLong

  }

}