package com.ibm

package plain

package io

import java.nio.ByteBuffer

/**
 * Make your class' factory extend BinaryFormatter to read/write an instance from/to a BinaryFormatByteBuffer.
 */
trait BinaryFormatter[T] {

  def serialize(elem: T, bytebuffer: BinaryFormatByteBuffer)

  def deserialize(bytebuffer: BinaryFormatByteBuffer): T

}

/**
 * Thrown whenever an unsupported binary format is read or written.
 */
object InvalidBinaryFormat extends Exception {

  override def fillInStackTrace = this

}

/**
 * Similar to [[java.io.DataOutput]], but not synchronized and simpler, supports Map and List.
 */
trait BinaryOutput {

  def writeBoolean(v: Boolean)
  def writeByte(v: Byte)
  /**
   * No more than 127 bytes.
   */
  def writeShortBytes(v: Array[Byte])
  def writeBytes(v: Array[Byte])
  def writeChar(v: Char)
  def writeShort(v: Short)
  def writeInt(v: Int)
  def writeLong(v: Long)
  def writeDouble(v: Double)
  /**
   * A short string has a length < 128 which fits into one byte.
   */
  def writeShortString(v: String)
  def writeString(v: String)
  def writeMap(v: Map[String, Any])
  def writeList(v: List[Any])
  def writeOption(v: Option[Any])

}

/**
 * Similar to [[java.io.DataInput]], but not synchronized and simpler, supports Map and List.
 */
trait BinaryInput {

  def readBoolean: Boolean
  def readByte: Byte
  /**
   * No more than 127 bytes.
   */
  def readShortBytes: Array[Byte]
  def readBytes: Array[Byte]
  def readChar: Char
  def readShort: Short
  def readInt: Int
  def readLong: Long
  def readDouble: Double
  /**
   * A short string has a length < 128 which fits into one byte.
   */
  def readShortString: String
  def readString: String
  def readMap: Map[String, Any]
  def readList: List[Any]
  def readOption: Option[Any]

}

/**
 * A binary formatted [[java.nio.ByteBuffer]], very fast (except write/read/String/ShortString), versatile (write/read/Map/List).
 */
final class BinaryFormatByteBuffer(
  private[this] final val buffer: ByteBuffer,
  private[this] final var position: Int,
  private[this] final val limit: Int)

  extends BinaryOutput with BinaryInput {

  def this(buf: ByteBuffer) = this(buf, 0, buf.capacity)

  def clear = { buffer.clear; position = initialposition }

  def getBuffer = buffer

  def writeBoolean(v: Boolean) = {
    buffer.put(advance(1), if (v) 1 else 0); ()
  }

  def writeByte(v: Byte) = {
    buffer.put(advance(1), v); ()
  }

  def writeShortBytes(v: Array[Byte]) = {
    writeByte(v.length.toByte)
    var i = 0; while (i < v.length) { buffer.put(advance(1), v(i)); i += 1 }
  }

  def writeBytes(v: Array[Byte]) = {
    writeInt(v.length)
    var i = 0; while (i < v.length) { buffer.put(advance(1), v(i)); i += 1 }
  }

  def writeChar(v: Char) = {
    buffer.putChar(advance(2), v); ()
  }

  def writeShort(v: Short) = {
    buffer.putShort(advance(2), v); ()
  }

  def writeInt(v: Int) = {
    buffer.putInt(advance(4), v); ()
  }

  def writeLong(v: Long) = {
    buffer.putLong(advance(8), v); ()
  }

  def writeDouble(v: Double) = {
    buffer.putLong(advance(8), java.lang.Double.doubleToLongBits(v)); ()
  }

  def writeShortString(v: String) = {
    writeByte(v.length.toByte)
    var i = 0; while (i < v.length) { buffer.putChar(advance(2), v.charAt(i)); i += 1 }
  }

  def writeString(v: String) = {
    /**
     * faster than more elegant variants
     */
    writeInt(v.length)
    var i = 0; while (i < v.length) { buffer.putChar(advance(2), v.charAt(i)); i += 1 }
  }

  def writeMap(v: Map[String, Any]) = {
    writeByte(v.size.toByte)
    v.toList.foreach {
      case (k, v) ⇒
        writeString(k)
        writeAny(v)
    }
  }

  def writeList(v: List[Any]) = {
    writeByte(v.size.toByte)
    v.foreach(writeAny)
  }

  def writeOption(v: Option[Any]) = {
    writeBoolean(v.isDefined)
    v match {
      case None ⇒
      case Some(v) ⇒ writeAny(v)
    }
  }

  def readBoolean: Boolean = {
    0 != buffer.get(advance(1))
  }

  def readByte: Byte = {
    buffer.get(advance(1))
  }

  def readShortBytes: Array[Byte] = {
    Array.fill(readByte)(readByte)
  }

  def readBytes: Array[Byte] = {
    Array.fill(readInt)(readByte)
  }

  def readChar: Char = {
    buffer.getChar(advance(2))
  }

  def readShort: Short = {
    buffer.getShort(advance(2))
  }

  def readInt: Int = {
    buffer.getInt(advance(4))
  }

  def readLong: Long = {
    buffer.getLong(advance(8))
  }

  def readDouble: Double = {
    java.lang.Double.longBitsToDouble(readLong)
  }

  def readShortString: String = {
    new String(Array.fill(readByte)(readChar))
  }

  def readString: String = {
    new String(Array.fill(readInt)(readChar))
  }

  def readMap: Map[String, Any] = {
    val len = readByte
    (0 until len).foldLeft(Map[String, Any]()) { case (m, _) ⇒ m ++ Map(readString -> readAny) }
  }

  def readList: List[Any] = {
    val len = readByte
    (0 until len).foldLeft(List[Any]()) { case (l, _) ⇒ l ++ List(readAny) }
  }

  def readOption: Option[Any] = {
    if (readBoolean) Some(readAny) else None
  }

  private[this] final def writeType(v: Any): Unit = {
    writeByte((v: @unchecked) match {
      case _: Boolean ⇒ 1
      case _: Byte ⇒ 2
      case _: Char ⇒ 3
      case _: Short ⇒ 4
      case _: Int ⇒ 5
      case _: Long ⇒ 6
      case _: Double ⇒ 7
      case v: String if 128 > v.length ⇒ 8
      case _: String ⇒ 9
      case _: Map[_, _] ⇒ 100
      case _: List[_] ⇒ 101
      case _: Option[_] ⇒ 102
      case _ ⇒ throw InvalidBinaryFormat
    })
  }

  private[this] final def writeAny(v: Any): Unit = {
    writeType(v)
    (v: @unchecked) match {
      case v: Boolean ⇒ writeBoolean(v)
      case v: Byte ⇒ writeByte(v)
      case v: Char ⇒ writeChar(v)
      case v: Short ⇒ writeShort(v)
      case v: Int ⇒ writeInt(v)
      case v: Long ⇒ writeLong(v)
      case v: Double ⇒ writeDouble(v)
      case v: String if 128 > v.length ⇒ writeShortString(v)
      case v: String ⇒ writeString(v)
      case v: Map[_, _] ⇒ writeMap(v.asInstanceOf[Map[String, Any]])
      case v: List[_] ⇒ writeList(v.asInstanceOf[List[Any]])
      case v: Option[_] ⇒ writeOption(v.asInstanceOf[Option[Any]])
      case _ ⇒ throw InvalidBinaryFormat
    }
  }

  private[this] final def readAny: Any = {
    readByte match {
      case 1 ⇒ readBoolean
      case 2 ⇒ readByte
      case 3 ⇒ readChar
      case 4 ⇒ readShort
      case 5 ⇒ readInt
      case 6 ⇒ readLong
      case 7 ⇒ readDouble
      case 8 ⇒ readShortString
      case 9 ⇒ readString
      case 100 ⇒ readMap
      case 101 ⇒ readList
      case 102 ⇒ readOption
      case invalid ⇒ throw InvalidBinaryFormat
    }
  }

  @inline private[this] final def advance(by: Int) = {
    val p = position
    position += by
    p
  }

  private[this] val initialposition = position

}

