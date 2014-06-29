package com.ibm

package plain

package jdbc

import java.sql.{ Date, PreparedStatement, ResultSet, Statement, Time, Timestamp, Types, Blob }
import java.sql.ResultSet.{ FETCH_FORWARD, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY }
import java.io.{ ByteArrayInputStream, InputStream, OutputStream }

import scala.language.implicitConversions

/**
 *
 */
object ConnectionHelper {

  /**
   * see also: https://wiki.scala-lang.org/display/SYGN/Simplifying-jdbc
   */

  /**
   * Return an Option.
   */
  implicit def rrs2Boolean(rs: RichResultSet) = rs.nextOptionBoolean
  implicit def rrs2Byte(rs: RichResultSet) = rs.nextOptionByte
  implicit def rrs2Int(rs: RichResultSet) = rs.nextOptionInt
  implicit def rrs2Long(rs: RichResultSet) = rs.nextOptionLong
  implicit def rrs2Float(rs: RichResultSet) = rs.nextOptionFloat
  implicit def rrs2Double(rs: RichResultSet) = rs.nextOptionDouble
  implicit def rrs2String(rs: RichResultSet) = rs.nextOptionString
  implicit def rrs2Date(rs: RichResultSet) = rs.nextOptionDate
  implicit def rrs2Time(rs: RichResultSet) = rs.nextOptionTime
  implicit def rrs2Timestamp(rs: RichResultSet) = rs.nextOptionTimestamp
  implicit def rrs2InputStream(rs: RichResultSet) = rs.nextOptionInputStream
  implicit def rrs2Array(rs: RichResultSet) = rs.nextOptionArray

  /**
   * Return a default value in case of None.
   */
  implicit final def rs2Boolean(rs: RichResultSet) = rs.nextOptionBoolean.getOrElse(false)
  implicit final def rs2Byte(rs: RichResultSet): Byte = rs.nextOptionByte.getOrElse(0)
  implicit final def rs2Int(rs: RichResultSet): Int = rs.nextOptionInt.getOrElse(0)
  implicit final def rs2Long(rs: RichResultSet): Long = rs.nextOptionLong.getOrElse(0L)
  implicit final def rs2Float(rs: RichResultSet) = rs.nextOptionFloat.getOrElse(0.0F)
  implicit final def rs2Double(rs: RichResultSet) = rs.nextOptionDouble.getOrElse(0.0)
  implicit final def rs2String(rs: RichResultSet) = rs.nextOptionString.getOrElse("")
  implicit final def rs2Date(rs: RichResultSet) = rs.nextOptionDate.getOrElse(Date.valueOf("1970-01-01"))
  implicit final def rs2Time(rs: RichResultSet) = rs.nextOptionTime.getOrElse(Time.valueOf("00:00:00"))
  implicit final def rs2Timestamp(rs: RichResultSet) = rs.nextOptionTimestamp.getOrElse(Timestamp.valueOf("1970-01-01 00:00:00.000000000"))
  implicit final def rs2InputStream(rs: RichResultSet) = rs.nextOptionInputStream.getOrElse(new ByteArrayInputStream(new Array[Byte](0)))
  implicit final def rs2Array(rs: RichResultSet) = rs.nextOptionArray.getOrElse(new Array[Byte](0))

  implicit final def resultSet2Rich(rs: ResultSet) = new RichResultSet(rs)

  implicit final def str2RichPrepared(s: String)(implicit conn: Connection) =
    new RichPreparedStatement(conn.prepareStatement(s, Array(CONCUR_READ_ONLY)))

  /**
   * Methods like map, list, dump and row should be used for debugging and rapid prototyping, whenever performance or memory consumption is an issue they must be avoided.
   */
  final class RichResultSet(private[this] val rs: ResultSet) {

    /**
     * Jdbc starts with 1 not with 0.
     */
    final def apply(i: Int) = { require(0 < i && i <= rs.getMetaData.getColumnCount); pos = i; this }

    final def nextBoolean: Boolean = { val ret = rs.getBoolean(pos); pos += 1; ret }
    final def nextByte: Byte = { val ret = rs.getByte(pos); pos += 1; ret }
    final def nextInt: Int = { val ret = rs.getInt(pos); pos += 1; ret }
    final def nextLong: Long = { val ret = rs.getLong(pos); pos += 1; ret }
    final def nextFloat: Float = { val ret = rs.getFloat(pos); pos += 1; ret }
    final def nextDouble: Double = { val ret = rs.getDouble(pos); pos += 1; ret }
    final def nextString: String = { val ret = rs.getString(pos); pos += 1; ret }
    final def nextDate: Date = { val ret = rs.getDate(pos); pos += 1; ret }
    final def nextTime: Time = { val ret = rs.getTime(pos); pos += 1; ret }
    final def nextTimestamp: Timestamp = { val ret = rs.getTimestamp(pos); pos += 1; ret }
    final def nextInputStream: InputStream = { val ret = rs.getBlob(pos); pos += 1; ret.getBinaryStream }
    final def nextArray: Array[Byte] = { val ret = rs.getBlob(pos); pos += 1; val r = ret.getBytes(1, ret.length.toInt); ret.free; r }

    final def nextOptionBoolean: Option[Boolean] = { val ret = rs.getBoolean(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionByte: Option[Byte] = { val ret = rs.getByte(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionInt: Option[Int] = { val ret = rs.getInt(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionLong: Option[Long] = { val ret = rs.getLong(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionFloat: Option[Float] = { val ret = rs.getFloat(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionDouble: Option[Double] = { val ret = rs.getDouble(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionString: Option[String] = { val ret = rs.getString(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionDate: Option[Date] = { val ret = rs.getDate(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionTime: Option[Time] = { val ret = rs.getTime(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionTimestamp: Option[Timestamp] = { val ret = rs.getTimestamp(pos); pos += 1; if (rs.wasNull) None else Some(ret) }
    final def nextOptionInputStream: Option[InputStream] = { val ret = rs.getBlob(pos); pos += 1; if (rs.wasNull) None else Some(ret.getBinaryStream) }
    final def nextOptionArray: Option[Array[Byte]] = { val ret = rs.getBlob(pos); pos += 1; if (rs.wasNull) None else { val r = Some(ret.getBytes(1, ret.length.toInt)); ret.free; r } }

    final def foldLeft[A](init: A)(f: (ResultSet, A) ⇒ A): A = rs.next match {
      case false ⇒ init
      case true  ⇒ foldLeft(f(rs, init))(f)
    }

    final def map[A](f: ResultSet ⇒ A): Seq[A] = {
      val buffer = new scala.collection.mutable.ListBuffer[A]
      do {
        pos = 1
        buffer += f(rs)
      } while (rs.next)
      buffer.toSeq
    }

    /**
     * Call json.Json.build with this to convert the whole resultset into a Json.JArray. Do not call this on large resultsets.
     */
    final def list: Seq[Map[String, Any]] = { var i = 0; map((rs: ResultSet) ⇒ { i += 1; row ++ Map("row" -> i) }) }

    final def row: Map[String, Any] = (for (i ← 1 to columncount) yield meta(i - 1) match { case (name, width, f) ⇒ (name, f(rs)(i)) }).toMap

    /**
     * Do not call this on large resultsets.
     */
    final def dump: String = {
      val n = columncount
      val format = new StringBuilder
      val buffer = new StringBuilder(io.defaultBufferSize)
      for (i ← 1 to n) format.append("%-").append(meta(i - 1)._2).append("s ")
      val headers = for (i ← 1 to n) yield meta(i - 1)._1
      buffer.append(format.toString.format(headers: _*)).append("\n")
      for (i ← 1 to n) buffer.append("-" * meta(i - 1)._2).append(" ")
      do {
        buffer.append("\n")
        import scala.collection.immutable.StringOps
        val r = for (i ← 1 to n) yield meta(i - 1) match { case (_, width, f) ⇒ new StringOps((f(rs)(i) match { case null ⇒ "null" case v ⇒ v }).toString).take(width) }
        buffer.append(format.toString.format(r: _*))
      } while (rs.next)
      buffer.toString
    }

    /**
     * Should be completed to handle all cases.
     */
    private[this] final def getter(pos: Int): (ResultSet ⇒ Int ⇒ Any) = rs.getMetaData.getColumnType(pos) match {
      case Types.VARCHAR ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getString(i)
      case Types.CHAR ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getString(i)
      case Types.INTEGER ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getInt(i)
      case Types.DOUBLE ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getDouble(i)
      case Types.BOOLEAN ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getBoolean(i)
      case Types.BLOB ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getBlob(i) match { case null ⇒ null case blob ⇒ val a = blob.getBytes(1, blob.length.toInt); blob.free; text.anyToBase64(a) }
      case Types.SMALLINT ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getShort(i)
      case Types.FLOAT ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getFloat(i)
      case Types.DATE ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getDate(i)
      case Types.TIME ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getTime(i)
      case Types.TIMESTAMP ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getTimestamp(i)
      case Types.BIGINT ⇒ (rs: ResultSet) ⇒ (i: Int) ⇒ rs.getBigDecimal(i)
      case t ⇒ unsupported
    }

    private[this] final def columncount = rs.getMetaData.getColumnCount

    private[this] final def meta = {
      val metadata = rs.getMetaData
      val width = 32
      val array = new Array[(String, Int, ResultSet ⇒ Int ⇒ Any)](columncount)
      import scala.math.max
      for (i ← 1 to columncount) array.update(i - 1, (metadata.getColumnName(i).toLowerCase, metadata.getColumnDisplaySize(i) match { case s if s > 127 ⇒ scala.math.max(width, metadata.getColumnName(i).length) case s ⇒ scala.math.max(s, metadata.getColumnName(i).length) }, getter(i)))
      array
    }

    private[this] final var pos = 1

  }

  /**
   *
   */
  final class RichPreparedStatement(

    private[this] val ps: PreparedStatement) {

    @inline final def execute[A](f: RichResultSet ⇒ A): Stream[A] = {
      pos = 1
      makestream(f, ps.executeQuery)
    }

    @inline final def execute: Boolean = { pos = 1; ps.execute }

    @inline final def executeUpdate: Int = { pos = 1; ps.executeUpdate }

    final def ! = execute((rs: RichResultSet) ⇒ rs)

    final def ![A](f: RichResultSet ⇒ A) = execute(f)

    final def !! = execute

    final def +! = executeUpdate

    final def ++ = ps.addBatch

    final def ++! = { pos = 1; ps.executeBatch }

    final def <<?(n: Int): RichPreparedStatement = { repeat = n; this }

    final def <<(any: Option[Any]): RichPreparedStatement = {
      any match {
        case None ⇒
          ps.setNull(pos, Types.NULL); pos += 1; this
        case Some(a) ⇒ (this << a)
      }
    }

    final def <<(any: Any): RichPreparedStatement = {
      while (0 < repeat) {
        any match {
          case a: Boolean     ⇒ ps.setBoolean(pos, a)
          case a: Byte        ⇒ ps.setByte(pos, a)
          case a: Int         ⇒ ps.setInt(pos, a)
          case a: Long        ⇒ ps.setLong(pos, a)
          case a: Float       ⇒ ps.setFloat(pos, a)
          case a: Double      ⇒ ps.setDouble(pos, a)
          case a: String      ⇒ ps.setString(pos, a)
          case a: Date        ⇒ ps.setDate(pos, a)
          case a: InputStream ⇒ ps.setBinaryStream(pos, a)
          case a: Array[Byte] ⇒ ps.setBytes(pos, a)
          case a              ⇒ ps.setObject(pos, a)
        }
        pos += 1
        repeat -= 1
      }
      this <<? 1
    }

    private[this] final var pos = 1

    private[this] final var repeat = 1

  }

  implicit final def query[A](s: String, f: RichResultSet ⇒ A)(implicit stat: Statement): Stream[A] = {
    makestream(f, stat.executeQuery(s))
  }

  implicit final def simpleQuery(s: String)(implicit stat: Statement): Stream[RichResultSet] = {
    query(s, (rs: RichResultSet) ⇒ rs)
  }

  /**
   * Glue it all together.
   */
  @inline private[this] final def makestream[A](f: RichResultSet ⇒ A, rs: ResultSet): Stream[A] = {
    if (rs.next) {
      Stream.cons(f(new RichResultSet(rs)), makestream(f, rs))
    } else {
      rs.close
      Stream.empty
    }
  }

}
