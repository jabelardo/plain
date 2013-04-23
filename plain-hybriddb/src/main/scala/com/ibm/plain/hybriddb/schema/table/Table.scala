package com.ibm

package plain

package hybriddb

package schema

package table

import scala.reflect._
import runtime._
import universe._
import scala.collection.SeqView

import column._

/**
 *
 */
trait Table

  extends Serializable {

  def name: String

  def length: Long

}

/**
 *
 */
object Table {

  import reflect.Helpers._
  import reflect.mirror._

  def fromSeq[T <: Table: TypeTag](name: String, capacity: Long, rows: Seq[Seq[Any]]): T = {
    val builders = createBuilders[T](capacity)
    rows.foreach { row ⇒
      var i = 0
      row.foreach { v ⇒ builders(i).nextAny(v); i += 1 }
    }
    val parameters: List[Any] = name :: capacity :: builders.map(_.result).toList
    newInstance[T](parameters)
  }

  private[this] final def createBuilders[T <: Table: TypeTag](capacity: Long): Seq[ColumnBuilder[_, _]] = {
    def newOrdering(o: Type) = runtimeClass(o).newInstance
    def newClassTag(c: Type) = ClassTag(runtimeClass(c))
    constructorParamsOfType[T, BuiltColumn[_]].map { c ⇒
      val TClassTag = TType[ClassTag[_]]
      val TOrdering = TType[Ordering[_]]
      val (columnclasstag, ordering) = typeArguments(c) match {
        case Nil ⇒ (null, null)
        case ord :: Nil if ord <:< typeOf[Ordering[_]] ⇒ (null, newOrdering(ord))
        case c :: Nil ⇒ (newClassTag(c), null)
        case c :: ord :: Nil if ord <:< typeOf[Ordering[_]] ⇒ (newClassTag(c), newOrdering(ord))
      }
      val builder = c.members.filter(m ⇒ m.isType && m.typeSignature <:< typeOf[ColumnBuilder[_, _]]).map(_.typeSignature).head
      val parameters = constructorParams(builder) match {
        case TLong +: TNil ⇒ List(capacity)
        case TLong +: TClassTag +: TNil ⇒ List(capacity, columnclasstag)
        case TLong +: TOrdering +: TNil ⇒ List(capacity, ordering)
        case TLong +: TOrdering +: TClassTag +: TNil ⇒ List(capacity, ordering, columnclasstag)
        case _ ⇒ unsupported
      }
      newInstance[ColumnBuilder[_, _]](builder, parameters)
    }
  }

}

/**
 *
 */
trait TableBuilder[T <: Table] {

  def result: T

}

