package com.ibm

package plain

package hybriddb

package schema

package table

import scala.collection.Seq

/**
 *
 */
sealed trait Row {

  def width: Int

  def get(columnindex: Int): Any

  final def apply[A](columnindex: Int) = get(columnindex)

}

/**
 *
 */
final class BaseRow private[table] (

  values: Seq[Any])

  extends Row {

  final val width = values.length

  final def get(columnindex: Int): Any = values(columnindex)

  override def toString = values.toString

}

/**
 *
 */
object Row {

  def apply(columnvalues: Seq[Any]): Row = new BaseRow(columnvalues)

  def apply(value: Any, values: Any*): Row = apply(value +: values)

}