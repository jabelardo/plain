package com.ibm

package plain

package hybriddb

package schema

package table

import scala.collection.Seq

import column.{ Column, ColumnBuilder }

/**
 *
 */
trait Table

  extends Serializable {

  def name: String

  def length: Long

  def width: Int

  def get(rowindex: Long): Row

  final def apply(rowindex: Long) = get(rowindex)

  def columns: Seq[Column[_]]

  def get(columnname: String): Column[_]

  final def apply(columnname: String) = get(name)

}

class BaseTable(

  val name: String,

  val length: Long,

  val width: Int,

  val columns: Seq[Column[_]])

  extends Table {

  override final def toString = "Table(name=" + name + " length=" + length + " width=" + width + " columns=" + columns + ")"

  def get(rowindex: Long): Row = Row(columns.map(_.get(rowindex)))

  def get(columnname: String): Column[_] = throw null

}

/**
 *
 */
object Table {

  def fromSeq(
    name: String,
    columnbuilders: Seq[ColumnBuilder[_, _]],
    rows: Seq[Seq[Any]]): Table = {
    val builder = new BaseTableBuilder(name, columnbuilders)
    rows.foreach(row ⇒ builder.next(Row(row)))
    builder.result
  }

}

/**
 *
 */
trait TableBuilder {

  def result: Table

  final def next(row: Row) = for (i ← 0 until width) columnbuilders(i).nextAny(row(i))

  final def apply(row: Row) = next(row)

  protected[this] def columnbuilders: Seq[ColumnBuilder[_, _]]

  protected[this] final def length: Long = columnbuilders(0).length

  protected[this] final def width: Int = columnbuilders.length

}

/**
 *
 */
final class BaseTableBuilder(

  name: String,

  val columnbuilders: Seq[ColumnBuilder[_, _]])

  extends TableBuilder {

  final def result: Table = new BaseTable(name, length, width, columnbuilders.map(_.result.asInstanceOf[Column[_]]))

}
