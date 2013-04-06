package com.ibm

package plain

package hybriddb

package schema

package table

import scala.util.Random._

import org.junit.Test
import collection.immutable.Sorting._
import column._

@Test class TestTable {

  @Test def test1 = {
    var i = -1
    val rows = new Array[Seq[Any]](5000000)
    for (i ← 0 until rows.length) { if (0 == i % 100000) println(i); rows.update(i, Seq(/*i, nextString(20), 0 == nextInt(2), nextInt(10).toString, */nextInt(100000).toLong)) }
    //    val roxws = Seq(Seq(1, "Peter", true, "Controlling", 100000L), Seq(2, "Paul", true, "Sales", 90000L), Seq(3, "Mary", false, "Sales", 200000L))
    val capacity = rows.length
    val columns = Seq(
/*      UniqueColumnBuilder[Int]("id", capacity),
      IndexedStringColumnBuilder("name", capacity, Ordering.String),
      BooleanColumnBuilder("male", capacity),
      BitSetColumnBuilder[String]("department", capacity),
*/      MemoryMappedColumnBuilder[Long]("salary", capacity, "/tmp/salary", Some(Ordering.Long)))
    println("create")
    println(time.timeMillis {
      val t = Table.fromSeq("test", columns, rows)
      //    for (i ← 0L until t.length) println(t(i))
      println(t)
    }._2)
    assert(true)
  }

}