package com.ibm

package plain

package hybriddb

package schema

package table

import org.junit.Test

import scala.language.reflectiveCalls

import shapeless._
import HList._
import Tuples._
import TypeOperators._

import column._
import json.Json

object Test2 {

  implicit class DoubleCase(val value: (Double, Double)) {

    def clone1 = new DoubleCase((value._1 + 42.0, value._2 + 43.0))

    def inc1 = value._1 + 1.0

    private val d = value._2 / 2.12

  }

  implicit final class DoubleVal(val value: (Double, Double)) extends AnyVal {

    final def clone2 = new DoubleVal((value._1 + 42.0, value._2 + 43.0))

    final def inc2 = value._1 + 1.0

  }

}

@Test class TestTable {

  @Test def test4 = {
    val data = List(List(1001, "Dow", "John", "London", true, 21), List(1002, "Smith", "Mary", "München", false, 23), List(1003, "Jones", "Paul", "London", false, 24))
    val p = Table.fromSeq[Persons]("persons", data.length, data.view)
    for (i ← 0 until p.length.toInt) println(i + " : " + Json.build(p.row(i)))
    assert(true)
  }

  @Test def test2 = {
    import Test2._
    import util.Random.nextDouble
    val max = 10000000
    var c = 0L
    var t = 0L
    var dd = 0.0
    var ds = 0.0
    for (j ← 1 to 10) {
      for (i ← 1 to max) {
        c += 1
        t += time.timeNanos { val d = (nextDouble, nextDouble).clone1.value; dd = d.inc1; ds += dd }._2
      }
      println("case class " + (t / c) + " " + t)
      c = 0L
      t = 0L
      for (i ← 1 to max) {
        c += 1
        t += time.timeNanos { val d = (nextDouble, nextDouble).clone2.value; dd = d.inc2; ds += dd }._2
      }
      println("val class " + (t / c) + " " + t)
    }
    println(ds)
  }

  @Test def test1 = {

    trait Assign[A] { def assign(a: Any): A }

    type ToInt = { def toInt: Int }

    class AssignInt extends Assign[Int] {
      def assign(a: Any): Int = a match {
        case a: Int ⇒ a
        case a: Double ⇒ scala.math.round(a).toInt
        case a: Float ⇒ scala.math.round(a)
        case a: String ⇒ a.toInt
        case a: Boolean ⇒ if (a) 1 else 0
        case null ⇒ 0
        case None ⇒ 0
        case a: ToInt ⇒ a.toInt
      }
    }

    class A { def toInt = 42 }
    val in: List[Any] = List(1, 2, 3, "4", "5", 3.0, 3.14, 9.99, 3.14f, 7.toByte, 'A', new A, true, false, null, None)
  }

}

final class MyIntOrdering extends Ordering.IntOrdering
final class MyStringOrdering extends Ordering.StringOrdering

@SerialVersionUID(1L) final case class Persons(
  name: String,
  length: Long,
  id: UniqueColumn[Int],
  lastname: IndexedStringColumn[MyStringOrdering],
  firstname: IndexedStringColumn[MyStringOrdering],
  city: DictionaryColumn[String],
  male: BooleanColumn,
  age: IndexedColumn[Int, MyIntOrdering])

  extends Table {

  final val female = new FunctionColumn[Boolean](length, !male(_))

  final val ageindays = new FunctionColumn[Int](length, 365 * age(_))

}

