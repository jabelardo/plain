package com.ibm

package plain

package hybriddb

package schema

package table

import java.io.File

import scala.reflect._
import runtime._
import universe._
import scala.collection.SeqView

import column._
import reflect.ReflectHelper._
import reflect.mirror._
import logging.HasLogger

/**
 *
 */
trait Table

  extends TableHelper

  with Serializable {

  val name: String

  val length: Long

  lazy val columns = reflectColumns

  def row(index: Long): Map[String, Any] = columns.mapValues(_.get(index))

}

/**
 *
 */
object Table

  extends TableHelper

  with HasLogger {

  def fromSeq[T <: Table: TypeTag](name: String, capacity: Long, rows: Seq[Seq[Any]]): T = {
    val builders = createBuilders[T](capacity)
    var length = 0
    rows.foreach { row ⇒
      var i = 0
      row.foreach { v ⇒ val b = builders(i); val bb = b.getClass.cast(b); bb.set(v); i += 1 }
      length += 1
      if (log.isDebugEnabled && 0 == length % 100000) debug("" + length)
    }
    if (log.isDebugEnabled) debug(length + " rows")
    val parameters: List[Any] = name :: length :: builders.map(_.result).toList
    val table = newInstance[T](parameters)
    serialize(table, new File("/tmp/persons.bin"))
    deserialize[T](new File("/tmp/persons.bin"))
  }

}

