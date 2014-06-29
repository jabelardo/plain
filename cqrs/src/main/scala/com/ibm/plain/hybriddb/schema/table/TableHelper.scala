package com.ibm

package plain

package hybriddb

package schema

package table

import java.io._

import scala.reflect._
import runtime._
import universe._
import universe.{ typeOf ⇒ utypeOf }

import column._
import reflect.ReflectHelper._
import reflect.mirror._
import plain.io.LZ4
import logging.Logger

protected trait TableHelper {

  protected[this] final def reflectColumns: Map[String, Column[_]] = {
    val T = reflect(this)
    valsOfType(typeOfInstance(this), utypeOf[Column[_]]).foldLeft(Map[String, Column[_]]()) { (m, c) ⇒
      m ++ Map(c.name.decodedName.toString.trim -> T.reflectField(c.asTerm).get.asInstanceOf[Column[_]])
    }
  }

  protected[this] final def createBuilders[T <: Table: TypeTag](capacity: Long): List[ColumnBuilder[_, _]] = {
    def newOrdering(o: Type) = runtimeClass(o).newInstance
    def newClassTag(c: Type) = ClassTag(runtimeClass(c))
    constructorParamsOfType[T, BuiltColumn[_]].map { c ⇒
      val TClassTag = TType[ClassTag[_]]
      val TOrdering = TType[Ordering[_]]
      val (columnclasstag, ordering) = typeArguments(c) match {
        case Nil ⇒ (null, null)
        case ord :: Nil if ord <:< utypeOf[Ordering[_]] ⇒ (null, newOrdering(ord))
        case c :: Nil ⇒ (newClassTag(c), null)
        case c :: ord :: Nil if ord <:< utypeOf[Ordering[_]] ⇒ (newClassTag(c), newOrdering(ord))
      }
      val builder = c.members.filter(m ⇒ m.isType && m.typeSignature <:< utypeOf[ColumnBuilder[_, _]]).map(_.typeSignature).head
      val parameters = constructorParams(builder) match {
        case TLong +: TNil                           ⇒ List(capacity)
        case TLong +: TClassTag +: TNil              ⇒ List(capacity, columnclasstag)
        case TLong +: TOrdering +: TNil              ⇒ List(capacity, ordering)
        case TLong +: TOrdering +: TClassTag +: TNil ⇒ List(capacity, ordering, columnclasstag)
        case _                                       ⇒ unsupported
      }
      newInstance[ColumnBuilder[_, _]](builder, parameters)
    }
  }

  protected[this] final def serialize(t: Table, f: File) = {
    val out = new ObjectOutputStream(LZ4.newFastOutputStream(new FileOutputStream(f)))
    try out.writeObject(t) finally out.close
  }

  protected[this] final def deserialize[T <: Table](f: File): T = {
    val in = new ObjectInputStream(LZ4.newInputStream(new FileInputStream(f)))
    try in.readObject.asInstanceOf[T] finally in.close
  }

}

object TableHelper {

}

