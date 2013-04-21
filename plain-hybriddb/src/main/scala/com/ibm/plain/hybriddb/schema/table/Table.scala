package com.ibm

package plain

package hybriddb

package schema

package table

import scala.language._
import scala.reflect._
import runtime._
import universe._
import scala.collection.Seq
import column._
import sun.reflect.generics.tree.TypeSignature

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

  import reflect.mirror._

  def fromSeq[T <: Table: TypeTag](rows: Seq[Seq[Any]]): T = {
    val builders_ = createBuilders[T](rows.length).toList
    val builders = new scala.collection.mutable.ListBuffer[ColumnBuilder[_, _]]
    builders += builders_(4) += builders_(2) += builders_(3) += builders_(1) += builders_(0)
    rows.foreach { row ⇒
      var i = 0
      row.foreach { v ⇒ builders(i).nextAny(v); i += 1 }
    }
    val tabletype = typeOf[T]
    val ctor = tabletype.declaration(nme.CONSTRUCTOR).asMethod
    val tableconstructor = reflectClass(tabletype.typeSymbol.asClass).reflectConstructor(ctor)
    val parameters: List[Any] = "persons" :: rows.length :: builders.map(_.result).toList
    tableconstructor(parameters: _*).asInstanceOf[T]
  }

  private[this] final def createBuilders[T <: Table: TypeTag](capacity: Long): Seq[ColumnBuilder[_, _]] = {
    val tabletype = typeOf[T]
    val columntype = typeOf[BuiltColumn[_]]
    val buildertype = typeOf[ColumnBuilder[_, _]]
    val columntypes = tabletype.members.filter(m ⇒ m.isTerm && !m.isMethod && m.typeSignature <:< columntype).map(_.typeSignature).toSeq
    columntypes.map { c ⇒
      val TypeRef(_, _, columntypeargs) = c
      val (columnclasstag, ordering) = columntypeargs match {
        case Nil ⇒ (null, null)
        case ord :: Nil if ord <:< typeOf[Ordering[_]] ⇒ (null, runtimeClass(ord).newInstance)
        case c :: ord :: Nil if ord <:< typeOf[Ordering[_]] ⇒ (ClassTag(runtimeClass(c)), runtimeClass(ord).newInstance)
        case c :: Nil ⇒ (ClassTag(runtimeClass(c)), null)
        case _ ⇒ throw new UnsupportedOperationException
      }
      val builder = c.members.filter(m ⇒ m.isType && m.typeSignature <:< buildertype).map(_.typeSignature).head
      val ctor = builder.declaration(nme.CONSTRUCTOR).asMethod
      val constructor = reflectClass(builder.typeSymbol.asClass).reflectConstructor(ctor)
      (constructor.symbol.paramss.flatten.map(_.typeSignature) match {
        case cap :: Nil if cap =:= typeOf[Long] ⇒ constructor(capacity)
        case cap :: ctag :: Nil if cap =:= typeOf[Long] && ctag <:< typeOf[ClassTag[_]] ⇒ constructor(capacity, columnclasstag)
        case cap :: ord :: Nil if cap =:= typeOf[Long] && ord <:< typeOf[Ordering[_]] ⇒ constructor(capacity, ordering)
        case cap :: ord :: ctag :: Nil if cap =:= typeOf[Long] && ctag <:< typeOf[ClassTag[_]] && ord <:< typeOf[Ordering[_]] ⇒ constructor(capacity, ordering, columnclasstag)
        case _ ⇒ throw new UnsupportedOperationException
      }).asInstanceOf[ColumnBuilder[_, _]]
    }
  }

}

/**
 *
 */
trait TableBuilder[T <: Table] {

  def result: T

}

