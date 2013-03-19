package com.ibm

package plain

package jdbc

import java.sql.{ Date, PreparedStatement, ResultSet, Statement, Time, Timestamp, Types, Blob }
import java.io.{ ByteArrayInputStream, InputStream, OutputStream }

import scala.language.implicitConversions

object ConnectionHelper {

  /**
   * see also: https://wiki.scala-lang.org/display/SYGN/Simplifying-jdbc
   */

  /**
   * Return an Option.
   */
  implicit def rrs2Boolean(rs: RichResultSet) = rs.nextBoolean
  implicit def rrs2Byte(rs: RichResultSet) = rs.nextByte
  implicit def rrs2Int(rs: RichResultSet) = rs.nextInt
  implicit def rrs2Long(rs: RichResultSet) = rs.nextLong
  implicit def rrs2Float(rs: RichResultSet) = rs.nextFloat
  implicit def rrs2Double(rs: RichResultSet) = rs.nextDouble
  implicit def rrs2String(rs: RichResultSet) = rs.nextString
  implicit def rrs2Date(rs: RichResultSet) = rs.nextDate
  implicit def rrs2Time(rs: RichResultSet) = rs.nextTime
  implicit def rrs2Timestamp(rs: RichResultSet) = rs.nextTimestamp
  implicit def rrs2InputStream(rs: RichResultSet) = rs.nextInputStream
  implicit def rrs2Array(rs: RichResultSet) = rs.nextArray

  /**
   * Return a default value in case of None.
   */
  implicit final def rs2Boolean(rs: RichResultSet) = rs.nextBoolean.getOrElse(false)
  implicit final def rs2Byte(rs: RichResultSet): Byte = rs.nextByte.getOrElse(0)
  implicit final def rs2Int(rs: RichResultSet): Int = rs.nextInt.getOrElse(0)
  implicit final def rs2Long(rs: RichResultSet): Long = rs.nextLong.getOrElse(0L)
  implicit final def rs2Float(rs: RichResultSet) = rs.nextFloat.getOrElse(0.0F)
  implicit final def rs2Double(rs: RichResultSet) = rs.nextDouble.getOrElse(0.0)
  implicit final def rs2String(rs: RichResultSet) = rs.nextString.getOrElse("")
  implicit final def rs2Date(rs: RichResultSet) = rs.nextDate.getOrElse(Date.valueOf("1970-01-01"))
  implicit final def rs2Time(rs: RichResultSet) = rs.nextTime.getOrElse(Time.valueOf("00:00:00"))
  implicit final def rs2Timestamp(rs: RichResultSet) = rs.nextTimestamp.getOrElse(Timestamp.valueOf("1970-01-01 00:00:00.000000000"))
  implicit final def rs2InputStream(rs: RichResultSet) = rs.nextInputStream.getOrElse(new ByteArrayInputStream(new Array[Byte](0)))
  implicit final def rs2Array(rs: RichResultSet) = rs.nextArray.getOrElse(new Array[Byte](0))

  implicit final def resultSet2Rich(rs: ResultSet) = new RichResultSet(rs)
  implicit final def rich2ResultSet(r: RichResultSet) = r.rs

  implicit final def ps2Rich(ps: PreparedStatement) = new RichPreparedStatement(ps)
  implicit final def rich2PS(r: RichPreparedStatement) = r.ps

  implicit final def str2RichPrepared(s: String)(implicit conn: Connection): RichPreparedStatement = conn.prepareStatement(s)
  implicit final def conn2Rich(conn: Connection) = new RichConnection(conn)

  implicit final def st2Rich(s: Statement) = new RichStatement(s)
  implicit final def rich2St(rs: RichStatement) = rs.s

  implicit final def conn2Statement(conn: Connection): Statement = conn.createStatement

  /**
   *
   */
  final class RichResultSet(val rs: ResultSet) {

    final def nextBoolean: Option[Boolean] = { val ret = rs.getBoolean(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextByte: Option[Byte] = { val ret = rs.getByte(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextInt: Option[Int] = { val ret = rs.getInt(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextLong: Option[Long] = { val ret = rs.getLong(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextFloat: Option[Float] = { val ret = rs.getFloat(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextDouble: Option[Double] = { val ret = rs.getDouble(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextString: Option[String] = { val ret = rs.getString(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextDate: Option[Date] = { val ret = rs.getDate(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextTime: Option[Time] = { val ret = rs.getTime(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextTimestamp: Option[Timestamp] = { val ret = rs.getTimestamp(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextInputStream: Option[InputStream] = { val ret = rs.getBlob(pos); pos += 1; if (rs.wasNull) None else Some(ret.getBinaryStream) }
    final def nextArray: Option[Array[Byte]] = { val ret = rs.getBlob(pos); pos += 1; if (rs.wasNull) None else { val r = Some(ret.getBytes(1, ret.length.toInt)); ret.free; r } }

    final def foldLeft[A](init: A)(f: (ResultSet, A) ⇒ A): A = rs.next match {
      case false ⇒ init
      case true ⇒ foldLeft(f(rs, init))(f)
    }

    final def map[A](f: ResultSet ⇒ A) = {
      var ret = List[A]()
      while (rs.next()) {
        ret = f(rs) :: ret
      }
      ret.reverse
    }

    final def apply(i: Int) = { require(0 < i && i < rs.getMetaData.getColumnCount); pos = i; this }

    private[this] final var pos = 1

  }

  /**
   *
   */
  final class RichPreparedStatement(val ps: PreparedStatement) {

    final def execute[A](f: RichResultSet ⇒ A): Stream[A] = {
      pos = 1
      makestream(f, ps.executeQuery)
    }

    final def execute: Boolean = { pos = 1; ps.execute }

    final def executeUpdate: Int = { pos = 1; ps.executeUpdate }

    final def !! = execute((rs: RichResultSet) ⇒ rs)

    final def <<![A](f: RichResultSet ⇒ A) = execute(f)

    final def <<! = execute

    final def <<!! = executeUpdate

    final def <<?(n: Int): RichPreparedStatement = { repeat = n; this }

    final def <<(any: Option[Any]): RichPreparedStatement = {
      any match {
        case None ⇒
          ps.setNull(pos, Types.NULL); inc
        case Some(a) ⇒ (this << a)
      }
    }

    final def <<(any: Any): RichPreparedStatement = {
      while (0 < repeat) {
        any match {
          case a: Boolean ⇒ ps.setBoolean(pos, a)
          case a: Byte ⇒ ps.setByte(pos, a)
          case a: Int ⇒ ps.setInt(pos, a)
          case a: Long ⇒ ps.setLong(pos, a)
          case a: Float ⇒ ps.setFloat(pos, a)
          case a: Double ⇒ ps.setDouble(pos, a)
          case a: String ⇒ ps.setString(pos, a)
          case a: Date ⇒ ps.setDate(pos, a)
          case a: InputStream ⇒ ps.setBinaryStream(pos, a)
          case a: Array[Byte] ⇒ ps.setBytes(pos, a)
          case a ⇒ ps.setObject(pos, a)
        }
        pos += 1
        repeat -= 1
      }
      this <<? 1
    }

    @inline private[this] final def inc = { pos += 1; this }

    private[this] final var pos = 1

    private[this] final var repeat = 1

  }

  /**
   *
   */
  class RichConnection(val conn: Connection) {
    final def <<(sql: String) = new RichStatement(conn.createStatement) << sql
    final def <<(sql: Seq[String]) = new RichStatement(conn.createStatement) << sql
  }

  /**
   *
   */
  class RichStatement(val s: Statement) {
    final def <<(sql: String) = { s.execute(sql); this }
    final def <<(sql: Seq[String]) = { for (x ← sql) s.execute(x); this }
  }

  /**
   *
   */
  implicit final def query[A](s: String, f: RichResultSet ⇒ A)(implicit stat: Statement): Stream[A] = {
    makestream(f, stat.executeQuery(s))
  }

  /**
   *
   */
  implicit final def simpleQuery(s: String)(implicit stat: Statement): Stream[RichResultSet] = {
    query(s, (rs: RichResultSet) ⇒ rs)
  }

  /**
   * Glue it all together.
   */
  private final def makestream[A](f: RichResultSet ⇒ A, rs: ResultSet): Stream[A] = {
    if (rs.next) {
      Stream.cons(f(new RichResultSet(rs)), makestream(f, rs))
    } else {
      rs.close
      Stream.empty
    }
  }

}
