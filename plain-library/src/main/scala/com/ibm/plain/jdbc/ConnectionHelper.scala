package com.ibm

package plain

package jdbc

import java.sql.{ Date, PreparedStatement, ResultSet, Statement, Time, Timestamp, Types }

import scala.language.implicitConversions

object ConnectionHelper {

  implicit final def conn2Statement(conn: Connection): Statement = conn.createStatement

  implicit final def rrs2Boolean(rs: RichResultSet) = rs.nextBoolean
  implicit final def rrs2Byte(rs: RichResultSet) = rs.nextByte
  implicit final def rrs2Int(rs: RichResultSet) = rs.nextInt
  implicit final def rrs2Long(rs: RichResultSet) = rs.nextLong
  implicit final def rrs2Float(rs: RichResultSet) = rs.nextFloat
  implicit final def rrs2Double(rs: RichResultSet) = rs.nextDouble
  implicit final def rrs2String(rs: RichResultSet) = rs.nextString
  implicit final def rrs2NString(rs: RichResultSet) = rs.nextNString
  implicit final def rrs2Date(rs: RichResultSet) = rs.nextDate
  implicit final def rrs2Time(rs: RichResultSet) = rs.nextTime
  implicit final def rrs2Timestamp(rs: RichResultSet) = rs.nextTimestamp

  implicit final def rs2Boolean(rs: RichResultSet) = rs.nextBoolean match { case Some(b) ⇒ b case _ ⇒ false }
  implicit final def rs2Byte(rs: RichResultSet): Byte = rs.nextByte match { case Some(b) ⇒ b case _ ⇒ 0 }
  implicit final def rs2Int(rs: RichResultSet): Int = rs.nextInt match { case Some(i) ⇒ i case _ ⇒ 0 }
  implicit final def rs2Long(rs: RichResultSet): Long = rs.nextLong match { case Some(l) ⇒ l case _ ⇒ 0 }
  implicit final def rs2Float(rs: RichResultSet) = rs.nextFloat match { case Some(f) ⇒ f case _ ⇒ 0.0F }
  implicit final def rs2Double(rs: RichResultSet) = rs.nextDouble match { case Some(d) ⇒ d case _ ⇒ 0.0 }
  implicit final def rs2String(rs: RichResultSet) = rs.nextString match { case Some(s) ⇒ s case _ ⇒ "" }
  implicit final def rs2NString(rs: RichResultSet): NString = rs.nextNString match { case Some(s) ⇒ s case _ ⇒ new NString("") }
  implicit final def rs2Date(rs: RichResultSet) = rs.nextDate match { case Some(d) ⇒ d case _ ⇒ Date.valueOf("1970-01-01") }
  implicit final def rs2Time(rs: RichResultSet) = rs.nextTime match { case Some(t) ⇒ t case _ ⇒ Time.valueOf("00:00:00") }
  implicit final def rs2Timestamp(rs: RichResultSet) = rs.nextTimestamp match { case Some(t) ⇒ t case _ ⇒ Timestamp.valueOf("1970-01-01 00:00:00.000000000") }

  implicit final def resultSet2Rich(rs: ResultSet) = new RichResultSet(rs)
  implicit final def rich2ResultSet(r: RichResultSet) = r.rs

  implicit final def ps2Rich(ps: PreparedStatement) = new RichPreparedStatement(ps)
  implicit final def rich2PS(r: RichPreparedStatement) = r.ps

  implicit final def str2RichPrepared(s: String)(implicit conn: Connection): RichPreparedStatement = conn.prepareStatement(s)
  implicit final def conn2Rich(conn: Connection) = new RichConnection(conn)

  implicit final def st2Rich(s: Statement) = new RichStatement(s)
  implicit final def rich2St(rs: RichStatement) = rs.s

  class NString(val s: String) extends Serializable {
    override final def toString = s
    override final def equals(other: Any) = s.equals(other.asInstanceOf[NString].s)
    override final def hashCode = s.hashCode
  }

  implicit final def nstring2s(ns: NString) = ns.s

  implicit final def s2nstring(s: String) = new NString(s)

  class RichResultSet(val rs: ResultSet) {
    var pos = 1

    final def apply(i: Int) = { pos = i; this }

    final def nextBoolean: Option[Boolean] = { val ret = rs.getBoolean(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextByte: Option[Byte] = { val ret = rs.getByte(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextInt: Option[Int] = { val ret = rs.getInt(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextLong: Option[Long] = { val ret = rs.getLong(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextFloat: Option[Float] = { val ret = rs.getFloat(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextDouble: Option[Double] = { val ret = rs.getDouble(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextString: Option[String] = { val ret = rs.getString(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextNString: Option[NString] = {
      val b = rs.getBytes(pos)
      val ret = if (null == b) {
        null
      } else {
        (0 until b.length).foreach { i ⇒ if (0 < b(i) && b(i) < 32) b.update(i, '.') }
        new String(b, "ISO-8859-1")
      }
      pos += 1
      if (rs.wasNull) None else Some(new NString(ret))
    }
    final def nextDate: Option[Date] = { val ret = rs.getDate(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextTime: Option[Time] = { val ret = rs.getTime(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextTimestamp: Option[Timestamp] = { val ret = rs.getTimestamp(pos); pos += 1; if (rs.wasNull) None else Some(ret) }

    final def foldLeft[X](init: X)(f: (ResultSet, X) ⇒ X): X = rs.next match {
      case false ⇒ init
      case true ⇒ foldLeft(f(rs, init))(f)
    }
    final def map[X](f: ResultSet ⇒ X) = {
      var ret = List[X]()
      while (rs.next()) {
        ret = f(rs) :: ret
      }
      ret.reverse
    }
  }

  class RichPreparedStatement(val ps: PreparedStatement) {
    var pos = 1
    var repeat = 1
    private final def inc = { pos += 1; this }

    final def execute[X](f: RichResultSet ⇒ X): Stream[X] = {
      pos = 1
      makestream(f, ps.executeQuery)
    }
    final def <<![X](f: RichResultSet ⇒ X): Stream[X] = execute(f)

    final def execute = { pos = 1; ps.execute }
    final def <<! = execute

    final def <<(x: Option[Any]): RichPreparedStatement = {
      x match {
        case None ⇒
          ps.setNull(pos, Types.NULL)
          inc
        case Some(y) ⇒ (this << y)
      }
    }
    final def <<?(n: Int): RichPreparedStatement = {
      repeat = n
      this
    }
    final def <<(x: Any): RichPreparedStatement = {
      while (0 < repeat) {
        x match {
          case z: Boolean ⇒
            ps.setBoolean(pos, z)
          case z: Byte ⇒
            ps.setByte(pos, z)
          case z: Int ⇒
            ps.setInt(pos, z)
          case z: Long ⇒
            ps.setLong(pos, z)
          case z: Float ⇒
            ps.setFloat(pos, z)
          case z: Double ⇒
            ps.setDouble(pos, z)
          case z: String ⇒
            ps.setString(pos, z)
          case z: Date ⇒
            ps.setDate(pos, z)
          case z ⇒ ps.setObject(pos, z)
        }
        inc
        repeat -= 1
      }
      this <<? 1
    }
  }

  class RichConnection(val conn: Connection) {
    final def <<(sql: String) = new RichStatement(conn.createStatement) << sql
    final def <<(sql: Seq[String]) = new RichStatement(conn.createStatement) << sql
  }

  class RichStatement(val s: Statement) {
    final def <<(sql: String) = { s.execute(sql); this }
    final def <<(sql: Seq[String]) = { for (x ← sql) s.execute(x); this }
  }

  private final def makestream[X](f: RichResultSet ⇒ X, rs: ResultSet): Stream[X] = {
    if (rs.next) {
      Stream.cons(f(new RichResultSet(rs)), makestream(f, rs))
    } else {
      rs.close
      Stream.empty
    }
  }

  implicit final def query[X](s: String, f: RichResultSet ⇒ X)(implicit stat: Statement): Stream[X] = {
    makestream(f, stat.executeQuery(s))
  }

  final def iso8601(timestamp: java.sql.Timestamp) = {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    format.format(timestamp)
  }

}
