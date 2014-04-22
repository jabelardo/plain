package com.ibm

package plain

package reflect

import scala.collection.Seq
import scala.language.implicitConversions
import scala.reflect.runtime.universe.{ Symbol, Type, TypeRef, TypeRefTag, TypeTag, termNames }

/**
 *
 */
object ReflectHelper {

  import mirror._

  def typeOfInstance(any: Any) = reflect(any).symbol.typeSignature

  def newInstance[A: TypeTag](parameters: Seq[Any]): A = newInstance[A](typeOf[A], parameters)

  def newInstance[A: TypeTag](a: Type, parameters: Seq[Any]): A =
    reflectClass(a.typeSymbol.asClass).reflectConstructor(a.decl(termNames.CONSTRUCTOR).asMethod)(parameters: _*).asInstanceOf[A]

  def valsOfType[A: TypeTag, C: TypeTag]: Seq[Symbol] = valsOfType(typeOf[A], typeOf[C])

  def valsOfType(a: Type, c: Type): Seq[Symbol] =
    a.members.filter(m ⇒ m.isTerm && !m.isMethod && m.typeSignature <:< c).toSeq

  def constructorParams[A: TypeTag]: TList = constructorParams(typeOf[A])

  def constructorParams(a: Type): TList =
    TList(reflectClass(a.typeSymbol.asClass).reflectConstructor(a.decl(termNames.CONSTRUCTOR).asMethod).symbol.paramLists.flatten.map(_.typeSignature))

  def constructorParamsOfType[A: TypeTag, C: TypeTag]: List[Type] = constructorParamsOfType(typeOf[A], typeOf[C])

  def constructorParamsOfType(a: Type, c: Type): List[Type] =
    reflectClass(a.typeSymbol.asClass).reflectConstructor(a.decl(termNames.CONSTRUCTOR).asMethod).symbol.paramLists.flatten.map(_.typeSignature).filter(_ <:< c)

  def typeArguments[A: TypeTag]: List[Type] = typeArguments(typeOf[A])

  def typeArguments(a: Type): List[Type] = {
    val TypeRef(_, _, typeargs) = a
    typeargs
  }

  final class TType(val t: Type) {
    override final def equals(other: Any) = other match {
      case b: TType ⇒ t <:< b.t.erasure
      case _ ⇒ false
    }
    override final def hashCode = t.hashCode
    override final def toString = t.toString
  }

  def TType[A: TypeTag] = new TType(typeOf[A])

  object TType {
    final def apply(t: Type) = new TType(t)
  }

  implicit final def type2TType(t: Type) = new TType(t)
  implicit final def tType2Type(ttype: TType) = ttype.t

  sealed trait TList

  final case class +:(head: TType, tail: TList) extends TList {
    def ::(h: TType) = ReflectHelper.+:(h, this)
    override final def toString = head + " +: " + tail
  }

  sealed trait TNil extends TList {
    def :::(h: TType) = ReflectHelper.+:(h, this)
    override final def toString = "TNil"
  }

  case object TNil extends TNil

  object TList {
    def apply(ts: List[Type]): TList = if (ts.isEmpty) TNil else +:(TType(ts.head), apply(ts.tail))
  }

  val TInt = TType[Int]
  val TLong = TType[Long]
  val TString = TType[String]

}