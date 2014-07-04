package com.ibm

package plain

package math

import java.io.PrintWriter

final class Matrix(val m: Array[Double])

  extends Serializable {

  final override def toString = asString

  final def write(writer: PrintWriter)(implicit prefix: String = "m") = writer.print(asString)

  final def asString(implicit prefix: String = "m") = {
    val s = """"%s1":%.15g,"%s2":%.15g,"%s3":%.15g,"%s4":%.15g,"%s5":%.15g,"%s6":%.15g,"%s7":%.15g,"%s8":%.15g,"%s9":%.15g,"%s10":%.15g,"%s11":%.15g,"%s12":%.15g"""
    s.format(
      prefix, m(0),
      prefix, m(1),
      prefix, m(2),
      prefix, m(3),
      prefix, m(4),
      prefix, m(5),
      prefix, m(6),
      prefix, m(7),
      prefix, m(8),
      prefix, m(9),
      prefix, m(10),
      prefix, m(11))
  }

  final def toMap(implicit prefix: String = "m") = List(
    (prefix + "1", m(0)),
    (prefix + "2", m(1)),
    (prefix + "3", m(2)),
    (prefix + "4", m(3)),
    (prefix + "5", m(4)),
    (prefix + "6", m(5)),
    (prefix + "7", m(6)),
    (prefix + "8", m(7)),
    (prefix + "9", m(8)),
    (prefix + "10", m(9)),
    (prefix + "11", m(10)),
    (prefix + "12", m(11))).toMap

}

object Matrix {

  final def apply(
    m1: Double,
    m2: Double,
    m3: Double,
    m4: Double,
    m5: Double,
    m6: Double,
    m7: Double,
    m8: Double,
    m9: Double,
    m10: Double,
    m11: Double,
    m12: Double): Matrix = apply(Array[Double](m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12))

  final def apply(m: Array[Double]) = new Matrix(m)

  final val unity = apply(Array[Double](1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0))

}

