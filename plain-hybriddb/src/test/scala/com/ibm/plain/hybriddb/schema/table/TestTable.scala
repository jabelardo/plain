package com.ibm

package plain

package hybriddb

package schema

package table

import scala.language._
import scala.reflect._
import runtime._
import universe._

import org.junit.Test

import shapeless._
import HList._
import Tuples._
import TypeOperators._

import column._

@Test class TestTable {

  @Test def test4 = {
    val data = List(List(1, "Dow", "John", true, 21), List(2, "Smith", "Mary", false, 23))
    val p = Table.fromSeq[Persons]("persons", data.length, data.view)
    for (i ← 0 until p.length.toInt) println(i + " : " + p.firstname(i) + " " + p.lastname(i) + " " + p.female(i) + " " + p.age(i))
    assert(true)
  }

}

final class MyIntOrdering extends Ordering.IntOrdering
final class MyStringOrdering extends Ordering.StringOrdering

final case class Persons(
  name: String,
  length: Long,
  id: UniqueColumn[Int],
  lastname: IndexedStringColumn[MyStringOrdering],
  firstname: IndexedStringColumn[MyStringOrdering],
  male: BooleanColumn,
  age: IndexedColumn[Int, MyIntOrdering])

  extends Table {

  val female = new FunctionColumn[Boolean](length, !male(_))

}

//case class PersonsBuilder(
//  name: String,
//  capacity: Long,
//  id: UniqueColumnBuilder[Int],
//  lastname: IndexedStringColumnBuilder,
//  firstname: IndexedStringColumnBuilder,
//  male: BooleanColumnBuilder,
//  age: IndexedColumnBuilder[Int])
//
//  extends TableBuilder[Persons] {
//
//  def next(row: (Int, String, String, Boolean, Byte)) = {
//    val t: Tuple5[Int, String, String, Boolean, Byte] = row
//    id.next(row._1)
//    lastname.next(row._2)
//    firstname.next(row._3)
//    male.next(row._4)
//    age.next(row._5)
//  }
//
//  def result: Persons = Persons(name, id.length, id.result, lastname.result, firstname.result, male.result, age.result)
//
//}

//final class Person(
//  name: String,
//  length: Long,
//  width: Int,
//  columns: Seq[Column[_]],
//  val id: UniqueColumn[Int],
//  val lastname: IndexedStringColumn,
//  val firstname: IndexedStringColumn,
//  val male: BooleanColumn) extends BaseTable(name, length, width, columns)

